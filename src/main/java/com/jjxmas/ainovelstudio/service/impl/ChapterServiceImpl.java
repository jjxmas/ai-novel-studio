package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterStreamEvent;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.service.ChapterContextAssembler;
import com.jjxmas.ainovelstudio.service.ChapterPostProcessService;
import com.jjxmas.ainovelstudio.service.ChapterService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.ProjectChapterGenerationQueue;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private final ProjectMapper projectMapper;
    private final OutlineMapper outlineMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;
    private final ChapterContextAssembler chapterContextAssembler;
    private final ChapterPostProcessService chapterPostProcessService;
    private final ProjectChapterGenerationQueue projectChapterGenerationQueue;
    private final ChapterConverter chapterConverter;
    private final TransactionTemplate transactionTemplate;

    public ChapterServiceImpl(
            ProjectMapper projectMapper,
            OutlineMapper outlineMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService,
            ChapterContextAssembler chapterContextAssembler,
            ChapterPostProcessService chapterPostProcessService,
            ProjectChapterGenerationQueue projectChapterGenerationQueue,
            ChapterConverter chapterConverter,
            TransactionTemplate transactionTemplate) {
        this.projectMapper = projectMapper;
        this.outlineMapper = outlineMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.chapterContextAssembler = chapterContextAssembler;
        this.chapterPostProcessService = chapterPostProcessService;
        this.projectChapterGenerationQueue = projectChapterGenerationQueue;
        this.chapterConverter = chapterConverter;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public List<ChapterResponse> listChapters(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        return chapterConverter.toResponseList(list(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByAsc(Chapter::getChapterNo)));
    }

    @Override
    @Transactional
    public void confirmChapterOutline(Long chapterId) {
        Chapter chapter = requireChapter(chapterId);
        requireConfirmedGlobalOutline(chapter.getProjectId());
        chapter.setConfirmedOutlineAt(LocalDateTime.now()).setStatus("content_pending");
        updateById(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse generateChapter(ChapterGenerateRequest request) {
        Chapter chapter = requireChapter(request.getChapterId());
        if (!chapter.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "CHAPTER_PROJECT_MISMATCH");
        }
        requireConfirmedGlobalOutline(chapter.getProjectId());

        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? chapter.getTitle()
                : request.getTitle();
        String outline = request.getOutline() == null || request.getOutline().isBlank()
                ? chapter.getOutline()
                : request.getOutline();
        ChapterContext context = chapterContextAssembler.assemble(
                chapter,
                title,
                outline,
                request.getRevisionAdvice());
        AiGenerateResult result = aiOrchestratorService.generateChapter(request.getModelConfigId(), context);
        String content = result == null ? "" : blankToEmpty(result.getContent());
        chapter.setTitle(title)
                .setOutline(outline)
                .setContent(content)
                .setWordCount(countWords(content))
                .setStatus("drafted");
        updateById(chapter);

        Map<String, Object> snapshot = chapterSnapshot(chapter);
        Long jobId = generationJobService.recordFinishedJob(
                chapter.getProjectId(),
                "chapter_generation",
                "chapter",
                chapter.getId(),
                request.getModelConfigId(),
                generationInput(title, outline, chapterContextAssembler.asLogMap(context)),
                generationOutput(snapshot, result));
        versionService.recordVersion(
                chapter.getProjectId(),
                "chapter",
                chapter.getId(),
                snapshot,
                "ai_generate",
                "AI 鐢熸垚绔犺妭姝ｆ枃",
                request.getModelConfigId(),
                jobId);
        scheduleChapterPostProcess(chapter.getId(), request.getModelConfigId(), null, null);
        return chapterConverter.toResponse(chapter);
    }

    @Override
    public Flux<ChapterStreamEvent> streamGenerateChapter(ChapterGenerateRequest request) {
        return projectChapterGenerationQueue.enqueue(request.getProjectId(), () -> doStreamGenerateChapter(request));
    }

    private Flux<ChapterStreamEvent> doStreamGenerateChapter(ChapterGenerateRequest request) {
        return Mono.fromCallable(() -> prepareChapterGeneration(request))
                .flatMapMany((prepared) -> {
                    StringBuilder content = new StringBuilder();
                    return Flux.just(ChapterStreamEvent.started("开始生成章节正文"))
                            .concatWith(aiOrchestratorService.streamChapter(request.getModelConfigId(), prepared.context())
                            .doOnNext(content::append)
                            .map(ChapterStreamEvent::chunk)
                            .concatWith(Flux.just(ChapterStreamEvent.postProcessing("post_processing")))
                            .concatWith(Mono.fromCallable(() -> finishGeneratedChapter(
                                    prepared,
                                    content.toString(),
                                    request.getModelConfigId()))));
                })
                .onErrorResume((ex) -> Flux.just(ChapterStreamEvent.error(errorMessage(ex))));
    }

    @Override
    @Transactional
    public ChapterResponse updateChapterContent(Long chapterId, ChapterContentUpdateRequest request) {
        Chapter chapter = requireChapter(chapterId);
        chapter.setContent(request.getContent())
                .setWordCount(countWords(request.getContent()))
                .setStatus("drafted");
        updateById(chapter);
        versionService.recordVersion(
                chapter.getProjectId(),
                "chapter",
                chapter.getId(),
                chapterSnapshot(chapter),
                "user_edit",
                request.getChangeNote() == null ? "鐢ㄦ埛鐩存帴淇敼绔犺妭姝ｆ枃" : request.getChangeNote(),
                null,
                null);
        scheduleChapterPostProcess(chapter.getId(), null, "manual_chapter_edit", request.getChangeNote());
        return chapterConverter.toResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse rewriteChapter(Long chapterId, ChapterRewriteRequest request) {
        Chapter chapter = requireChapter(chapterId);
        if (chapter.getContent() == null || chapter.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "CHAPTER_CONTENT_REQUIRED");
        }
        ChapterContext context = chapterContextAssembler.assemble(
                chapter,
                chapter.getTitle(),
                chapter.getOutline(),
                request.getInstruction());
        AiGenerateResult result = aiOrchestratorService.rewriteChapter(
                request.getModelConfigId(),
                context,
                chapter.getContent());
        String content = result == null ? "" : blankToEmpty(result.getContent());
        chapter.setContent(content)
                .setWordCount(countWords(content))
                .setStatus("drafted");
        updateById(chapter);

        Map<String, Object> snapshot = chapterSnapshot(chapter);
        Long jobId = generationJobService.recordFinishedJob(
                chapter.getProjectId(),
                "chapter_rewrite",
                "chapter",
                chapter.getId(),
                request.getModelConfigId(),
                rewriteInput(request.getInstruction(), chapterContextAssembler.asLogMap(context)),
                generationOutput(snapshot, result));
        versionService.recordVersion(
                chapter.getProjectId(),
                "chapter",
                chapter.getId(),
                snapshot,
                "ai_rewrite",
                "AI rewritten chapter content",
                request.getModelConfigId(),
                jobId);
        scheduleChapterPostProcess(chapter.getId(), request.getModelConfigId(), "chapter_rewrite", request.getInstruction());
        return chapterConverter.toResponse(chapter);
    }

    @Override
    public Flux<ChapterStreamEvent> streamRewriteChapter(Long chapterId, ChapterRewriteRequest request) {
        Chapter chapter = requireChapter(chapterId);
        return projectChapterGenerationQueue.enqueue(chapter.getProjectId(), () -> doStreamRewriteChapter(chapterId, request));
    }

    private Flux<ChapterStreamEvent> doStreamRewriteChapter(Long chapterId, ChapterRewriteRequest request) {
        return Mono.fromCallable(() -> prepareChapterRewrite(chapterId, request))
                .flatMapMany((prepared) -> {
                    StringBuilder content = new StringBuilder();
                    return Flux.just(ChapterStreamEvent.started("开始重写章节正文"))
                            .concatWith(aiOrchestratorService.streamRewriteChapter(
                                    request.getModelConfigId(),
                                    prepared.context(),
                                    prepared.originalContent())
                            .doOnNext(content::append)
                            .map(ChapterStreamEvent::chunk)
                            .concatWith(Flux.just(ChapterStreamEvent.postProcessing("post_processing")))
                            .concatWith(Mono.fromCallable(() -> finishRewrittenChapter(
                                    prepared,
                                    content.toString(),
                                    request.getModelConfigId()))));
                })
                .onErrorResume((ex) -> Flux.just(ChapterStreamEvent.error(errorMessage(ex))));
    }

    private PreparedChapterGeneration prepareChapterGeneration(ChapterGenerateRequest request) {
        Chapter chapter = requireChapter(request.getChapterId());
        if (!chapter.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "CHAPTER_PROJECT_MISMATCH");
        }
        requireConfirmedGlobalOutline(chapter.getProjectId());
        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? chapter.getTitle()
                : request.getTitle();
        String outline = request.getOutline() == null || request.getOutline().isBlank()
                ? chapter.getOutline()
                : request.getOutline();
        ChapterContext context = chapterContextAssembler.assemble(
                chapter,
                title,
                outline,
                request.getRevisionAdvice());
        return new PreparedChapterGeneration(chapter, title, outline, context);
    }

    private PreparedChapterRewrite prepareChapterRewrite(Long chapterId, ChapterRewriteRequest request) {
        Chapter chapter = requireChapter(chapterId);
        if (chapter.getContent() == null || chapter.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "CHAPTER_CONTENT_REQUIRED");
        }
        ChapterContext context = chapterContextAssembler.assemble(
                chapter,
                chapter.getTitle(),
                chapter.getOutline(),
                request.getInstruction());
        return new PreparedChapterRewrite(chapter, request.getInstruction(), chapter.getContent(), context);
    }

    private ChapterStreamEvent finishGeneratedChapter(
            PreparedChapterGeneration prepared,
            String content,
            Long modelConfigId) {
        Chapter chapter = transactionTemplate.execute((status) -> {
            Chapter updatedChapter = prepared.chapter();
            updatedChapter.setTitle(prepared.title())
                    .setOutline(prepared.outline())
                    .setContent(content)
                    .setWordCount(countWords(content))
                    .setStatus("drafted");
            updateById(updatedChapter);

            AiGenerateResult result = streamResult(content);
            Map<String, Object> snapshot = chapterSnapshot(updatedChapter);
            Long jobId = generationJobService.recordFinishedJob(
                    updatedChapter.getProjectId(),
                    "chapter_generation",
                    "chapter",
                    updatedChapter.getId(),
                    modelConfigId,
                    generationInput(prepared.title(), prepared.outline(), chapterContextAssembler.asLogMap(prepared.context())),
                    generationOutput(snapshot, result));
            versionService.recordVersion(
                    updatedChapter.getProjectId(),
                    "chapter",
                    updatedChapter.getId(),
                    snapshot,
                    "ai_generate",
                    "AI streamed chapter content",
                    modelConfigId,
                    jobId);
            return updatedChapter;
        });
        chapterPostProcessService.refreshChapter(chapter.getId(), modelConfigId);
        return ChapterStreamEvent.done(chapterConverter.toResponse(chapter));
    }

    private ChapterStreamEvent finishRewrittenChapter(
            PreparedChapterRewrite prepared,
            String content,
            Long modelConfigId) {
        Chapter chapter = transactionTemplate.execute((status) -> {
            Chapter updatedChapter = prepared.chapter();
            updatedChapter.setContent(content)
                    .setWordCount(countWords(content))
                    .setStatus("drafted");
            updateById(updatedChapter);

            AiGenerateResult result = streamResult(content);
            Map<String, Object> snapshot = chapterSnapshot(updatedChapter);
            Long jobId = generationJobService.recordFinishedJob(
                    updatedChapter.getProjectId(),
                    "chapter_rewrite",
                    "chapter",
                    updatedChapter.getId(),
                    modelConfigId,
                    rewriteInput(prepared.instruction(), chapterContextAssembler.asLogMap(prepared.context())),
                    generationOutput(snapshot, result));
            versionService.recordVersion(
                    updatedChapter.getProjectId(),
                    "chapter",
                    updatedChapter.getId(),
                    snapshot,
                    "ai_rewrite",
                    "AI streamed chapter rewrite",
                    modelConfigId,
                    jobId);
            return updatedChapter;
        });
        chapterPostProcessService.refreshChapterAndMarkDirty(
                chapter.getId(),
                modelConfigId,
                "chapter_rewrite",
                prepared.instruction());
        return ChapterStreamEvent.done(chapterConverter.toResponse(chapter));
    }

    private AiGenerateResult streamResult(String content) {
        return AiGenerateResult.builder()
                .success(true)
                .content(content)
                .usage(Map.of("stream", true))
                .build();
    }

    private String errorMessage(Throwable ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "绔犺妭娴佸紡鐢熸垚澶辫触"
                : ex.getMessage();
    }

    private record PreparedChapterGeneration(
            Chapter chapter,
            String title,
            String outline,
            ChapterContext context) {
    }

    private record PreparedChapterRewrite(
            Chapter chapter,
            String instruction,
            String originalContent,
            ChapterContext context) {
    }

    private void scheduleChapterPostProcess(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote) {
        Runnable task = () -> {
            if (dirtyReason == null || dirtyReason.isBlank()) {
                chapterPostProcessService.refreshChapterAsync(chapterId, modelConfigId);
            } else {
                chapterPostProcessService.refreshChapterAndMarkDirtyAsync(chapterId, modelConfigId, dirtyReason, dirtyNote);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private Chapter requireChapter(Long chapterId) {
        Chapter chapter = getById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "CHAPTER_NOT_FOUND");
        }
        return chapter;
    }

    private void requireConfirmedGlobalOutline(Long projectId) {
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .isNotNull(Outline::getConfirmedAt)
                .last("LIMIT 1"));
        if (outline == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "没有大纲");
        }
    }

    private Map<String, Object> chapterSnapshot(Chapter chapter) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("chapterNo", chapter.getChapterNo() == null ? 0 : chapter.getChapterNo());
        snapshot.put("title", blankToEmpty(chapter.getTitle()));
        snapshot.put("outline", blankToEmpty(chapter.getOutline()));
        snapshot.put("content", blankToEmpty(chapter.getContent()));
        snapshot.put("wordCount", chapter.getWordCount() == null ? 0 : chapter.getWordCount());
        snapshot.put("status", blankToEmpty(chapter.getStatus()));
        return snapshot;
    }

    private Map<String, Object> generationInput(String title, String outline, Map<String, Object> context) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("title", blankToEmpty(title));
        input.put("outline", blankToEmpty(outline));
        input.put("context", context);
        return input;
    }

    private Map<String, Object> rewriteInput(String instruction, Map<String, Object> context) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("instruction", blankToEmpty(instruction));
        input.put("context", context);
        return input;
    }

    private Map<String, Object> generationOutput(Map<String, Object> snapshot, AiGenerateResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("chapter", snapshot);
        output.put("modelName", result == null ? "" : blankToEmpty(result.getModelName()));
        output.put("usage", result == null || result.getUsage() == null ? Map.of() : result.getUsage());
        return output;
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.replaceAll("\\s+", "").length();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
