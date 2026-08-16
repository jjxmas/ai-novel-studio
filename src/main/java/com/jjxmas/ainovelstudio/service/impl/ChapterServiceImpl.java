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
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerationResult;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterQualityCheckResult;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterCatalogResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterPageResponse;
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
    public List<ChapterCatalogResponse> listChapterCatalog(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        return chapterConverter.toCatalogResponseList(list(catalogQuery(projectId)
                .orderByAsc(Chapter::getChapterNo)));
    }

    @Override
    public ChapterPageResponse listChapters(Long projectId, String keyword, int page, int size) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "章节分页参数无效");
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<Chapter> countQuery = new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .and(!normalizedKeyword.isBlank(), wrapper -> applyKeyword(wrapper, normalizedKeyword));
        long total = count(countQuery);
        int offset = (page - 1) * size;
        LambdaQueryWrapper<Chapter> pageQuery = catalogQuery(projectId)
                .and(!normalizedKeyword.isBlank(), wrapper -> applyKeyword(wrapper, normalizedKeyword))
                .orderByAsc(Chapter::getChapterNo)
                .last("LIMIT " + size + " OFFSET " + offset);
        return ChapterPageResponse.builder()
                .items(chapterConverter.toCatalogResponseList(list(pageQuery)))
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    @Override
    public ChapterResponse getChapter(Long chapterId) {
        return chapterConverter.toResponse(requireChapter(chapterId));
    }

    private LambdaQueryWrapper<Chapter> catalogQuery(Long projectId) {
        return new LambdaQueryWrapper<Chapter>()
                .select(
                        Chapter::getId,
                        Chapter::getProjectId,
                        Chapter::getVolumeId,
                        Chapter::getStoryArcId,
                        Chapter::getChapterNo,
                        Chapter::getTitle,
                        Chapter::getOutline,
                        Chapter::getScenePlan,
                        Chapter::getWordCount,
                        Chapter::getStatus,
                        Chapter::getContentStatus,
                        Chapter::getConfirmedOutlineAt,
                        Chapter::getContentGeneratedAt,
                        Chapter::getContentUpdatedAt,
                        Chapter::getLastGenerationJobId,
                        Chapter::getLastContentVersionNo,
                        Chapter::getCheckedAt)
                .eq(Chapter::getProjectId, projectId);
    }

    private void applyKeyword(LambdaQueryWrapper<Chapter> wrapper, String keyword) {
        wrapper.like(Chapter::getTitle, keyword)
                .or()
                .like(Chapter::getOutline, keyword);
        try {
            wrapper.or().eq(Chapter::getChapterNo, Integer.parseInt(keyword));
        } catch (NumberFormatException ignored) {
            // 非数字关键词只搜索标题和大纲。
        }
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
        int expectedVersion = contentVersion(chapter);
        AiGenerateResult result = aiOrchestratorService.generateChapter(request.getModelConfigId(), context);
        String content = requireGeneratedContent(result);
        Chapter persisted = transactionTemplate.execute(status -> {
            Chapter lockedChapter = lockChapterForContent(chapter.getProjectId(), chapter.getId(), expectedVersion);
            lockedChapter.setTitle(title)
                    .setOutline(outline)
                    .setContent(content)
                    .setWordCount(countWords(content))
                    .setStatus("drafted");
            markGeneratedContent(lockedChapter);

            Map<String, Object> snapshot = chapterSnapshot(lockedChapter);
            Long jobId = generationJobService.recordFinishedJob(
                    lockedChapter.getProjectId(),
                    "chapter_generation",
                    "chapter",
                    lockedChapter.getId(),
                    request.getModelConfigId(),
                    generationInput(title, outline, chapterContextAssembler.asLogMap(context)),
                    generationOutput(snapshot, result));
            lockedChapter.setLastGenerationJobId(jobId);
            int versionNo = versionService.recordVersion(
                    lockedChapter.getProjectId(),
                    "chapter",
                    lockedChapter.getId(),
                    snapshot,
                    "ai_generate",
                    "AI 鐢熸垚绔犺妭姝ｆ枃",
                    request.getModelConfigId(),
                    jobId);
            lockedChapter.setLastContentVersionNo(versionNo);
            updateById(lockedChapter);
            scheduleChapterPostProcess(lockedChapter.getId(), request.getModelConfigId(), null, null);
            return lockedChapter;
        });
        if (persisted == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "章节正文保存失败");
        }
        return chapterConverter.toResponse(persisted);
    }

    @Override
    public ChapterGenerationResult generateChapterForBatch(ChapterGenerateRequest request) {
        PreparedChapterGeneration prepared = prepareChapterGeneration(request);
        AiGenerateResult result = aiOrchestratorService.generateChapter(request.getModelConfigId(), prepared.context());
        String content = requireGeneratedContent(result);
        PersistedChapterGeneration persisted = transactionTemplate.execute(status -> {
            Chapter chapter = lockChapterForContent(
                    prepared.chapter().getProjectId(),
                    prepared.chapter().getId(),
                    prepared.expectedVersion());
            chapter.setTitle(prepared.title())
                    .setOutline(prepared.outline())
                    .setContent(content)
                    .setWordCount(countWords(content))
                    .setStatus("drafted");
            markGeneratedContent(chapter);
            Map<String, Object> snapshot = chapterSnapshot(chapter);
            Long jobId = generationJobService.recordFinishedJob(
                    chapter.getProjectId(),
                    "chapter_generation",
                    "chapter",
                    chapter.getId(),
                    request.getModelConfigId(),
                    generationInput(prepared.title(), prepared.outline(), chapterContextAssembler.asLogMap(prepared.context())),
                    generationOutput(snapshot, result));
            chapter.setLastGenerationJobId(jobId);
            int versionNo = versionService.recordVersion(
                    chapter.getProjectId(),
                    "chapter",
                    chapter.getId(),
                    snapshot,
                    "ai_generate",
                    "AI batch-generated chapter content",
                    request.getModelConfigId(),
                    jobId);
            chapter.setLastContentVersionNo(versionNo);
            updateById(chapter);
            return new PersistedChapterGeneration(chapter, jobId);
        });
        if (persisted == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "章节正文保存失败");
        }
        ChapterQualityCheckResult qualityCheck = chapterPostProcessService.refreshChapter(
                persisted.chapter().getId(),
                request.getModelConfigId());
        return new ChapterGenerationResult(
                chapterConverter.toResponse(persisted.chapter()),
                persisted.generationJobId(),
                qualityCheck);
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
        Chapter currentChapter = requireChapter(chapterId);
        Chapter chapter = lockChapterForContent(
                currentChapter.getProjectId(), chapterId, request.getExpectedVersion());
        chapter.setContent(request.getContent())
                .setWordCount(countWords(request.getContent()))
                .setStatus("drafted")
                .setContentStatus("edited")
                .setContentUpdatedAt(LocalDateTime.now());
        int versionNo = versionService.recordVersion(
                chapter.getProjectId(),
                "chapter",
                chapter.getId(),
                chapterSnapshot(chapter),
                "user_edit",
                request.getChangeNote() == null ? "鐢ㄦ埛鐩存帴淇敼绔犺妭姝ｆ枃" : request.getChangeNote(),
                null,
                null);
        chapter.setLastContentVersionNo(versionNo);
        updateById(chapter);
        scheduleChapterPostProcess(chapter.getId(), null, "manual_chapter_edit", request.getChangeNote());
        return chapterConverter.toResponse(chapter);
    }

    @Override
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
        int expectedVersion = contentVersion(chapter);
        AiGenerateResult result = aiOrchestratorService.rewriteChapter(
                request.getModelConfigId(),
                context,
                chapter.getContent());
        String content = requireGeneratedContent(result);
        Chapter persisted = transactionTemplate.execute(status -> {
            Chapter lockedChapter = lockChapterForContent(chapter.getProjectId(), chapter.getId(), expectedVersion);
            lockedChapter.setContent(content)
                    .setWordCount(countWords(content))
                    .setStatus("drafted");
            markGeneratedContent(lockedChapter);

            Map<String, Object> snapshot = chapterSnapshot(lockedChapter);
            Long jobId = generationJobService.recordFinishedJob(
                    lockedChapter.getProjectId(),
                    "chapter_rewrite",
                    "chapter",
                    lockedChapter.getId(),
                    request.getModelConfigId(),
                    rewriteInput(request.getInstruction(), chapterContextAssembler.asLogMap(context)),
                    generationOutput(snapshot, result));
            lockedChapter.setLastGenerationJobId(jobId);
            int versionNo = versionService.recordVersion(
                    lockedChapter.getProjectId(),
                    "chapter",
                    lockedChapter.getId(),
                    snapshot,
                    "ai_rewrite",
                    "AI rewritten chapter content",
                    request.getModelConfigId(),
                    jobId);
            lockedChapter.setLastContentVersionNo(versionNo);
            updateById(lockedChapter);
            scheduleChapterPostProcess(lockedChapter.getId(), request.getModelConfigId(), "chapter_rewrite", request.getInstruction());
            return lockedChapter;
        });
        if (persisted == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "章节正文保存失败");
        }
        return chapterConverter.toResponse(persisted);
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
        return new PreparedChapterGeneration(chapter, title, outline, context, contentVersion(chapter));
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
        return new PreparedChapterRewrite(
                chapter,
                request.getInstruction(),
                chapter.getContent(),
                context,
                contentVersion(chapter));
    }

    private ChapterStreamEvent finishGeneratedChapter(
            PreparedChapterGeneration prepared,
            String content,
            Long modelConfigId) {
        String validContent = requireGeneratedContent(streamResult(content));
        Chapter chapter = transactionTemplate.execute((status) -> {
            Chapter updatedChapter = lockChapterForContent(
                    prepared.chapter().getProjectId(),
                    prepared.chapter().getId(),
                    prepared.expectedVersion());
            updatedChapter.setTitle(prepared.title())
                    .setOutline(prepared.outline())
                    .setContent(validContent)
                    .setWordCount(countWords(validContent))
                    .setStatus("drafted");
            markGeneratedContent(updatedChapter);

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
            updatedChapter.setLastGenerationJobId(jobId);
            int versionNo = versionService.recordVersion(
                    updatedChapter.getProjectId(),
                    "chapter",
                    updatedChapter.getId(),
                    snapshot,
                    "ai_generate",
                    "AI streamed chapter content",
                    modelConfigId,
                    jobId);
            updatedChapter.setLastContentVersionNo(versionNo);
            updateById(updatedChapter);
            return updatedChapter;
        });
        chapterPostProcessService.refreshChapter(chapter.getId(), modelConfigId);
        return ChapterStreamEvent.done(chapterConverter.toResponse(chapter));
    }

    private ChapterStreamEvent finishRewrittenChapter(
            PreparedChapterRewrite prepared,
            String content,
            Long modelConfigId) {
        String validContent = requireGeneratedContent(streamResult(content));
        Chapter chapter = transactionTemplate.execute((status) -> {
            Chapter updatedChapter = lockChapterForContent(
                    prepared.chapter().getProjectId(),
                    prepared.chapter().getId(),
                    prepared.expectedVersion());
            updatedChapter.setContent(validContent)
                    .setWordCount(countWords(validContent))
                    .setStatus("drafted");
            markGeneratedContent(updatedChapter);

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
            updatedChapter.setLastGenerationJobId(jobId);
            int versionNo = versionService.recordVersion(
                    updatedChapter.getProjectId(),
                    "chapter",
                    updatedChapter.getId(),
                    snapshot,
                    "ai_rewrite",
                    "AI streamed chapter rewrite",
                    modelConfigId,
                    jobId);
            updatedChapter.setLastContentVersionNo(versionNo);
            updateById(updatedChapter);
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

    private void markGeneratedContent(Chapter chapter) {
        LocalDateTime now = LocalDateTime.now();
        chapter.setContentStatus("generated")
                .setContentGeneratedAt(now)
                .setContentUpdatedAt(now);
    }

    private String requireGeneratedContent(AiGenerateResult result) {
        String content = result == null ? "" : blankToEmpty(result.getContent());
        if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || content.isBlank()) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "模型未返回有效的章节正文");
        }
        return content;
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
            ChapterContext context,
            int expectedVersion) {
    }

    private record PersistedChapterGeneration(Chapter chapter, Long generationJobId) {
    }

    private record PreparedChapterRewrite(
            Chapter chapter,
            String instruction,
            String originalContent,
            ChapterContext context,
            int expectedVersion) {
    }

    private void scheduleChapterPostProcess(Long chapterId, Long modelConfigId, String dirtyReason, String dirtyNote) {
        Runnable task = () -> {
            if (dirtyReason == null || dirtyReason.isBlank()) {
                chapterPostProcessService.enqueueChapter(chapterId, modelConfigId);
            } else {
                chapterPostProcessService.enqueueChapterAndMarkDirty(chapterId, modelConfigId, dirtyReason, dirtyNote);
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

    private Chapter lockChapterForContent(Long projectId, Long chapterId, Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "CHAPTER_CONTENT_VERSION_REQUIRED");
        }
        if (projectMapper.lockById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        Chapter chapter = baseMapper.selectByIdForUpdate(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "CHAPTER_NOT_FOUND");
        }
        if (contentVersion(chapter) != expectedVersion) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "CHAPTER_CONTENT_VERSION_CONFLICT");
        }
        return chapter;
    }

    private int contentVersion(Chapter chapter) {
        return chapter.getLastContentVersionNo() == null ? 0 : chapter.getLastContentVersionNo();
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
        snapshot.put("contentStatus", blankToEmpty(chapter.getContentStatus()));
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
