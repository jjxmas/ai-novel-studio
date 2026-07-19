package com.jjxmas.ainovelstudio.module.chapter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.module.chapter.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.module.chapter.entity.Chapter;
import com.jjxmas.ainovelstudio.module.chapter.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.module.chapter.service.ChapterService;
import com.jjxmas.ainovelstudio.module.generation.service.GenerationJobService;
import com.jjxmas.ainovelstudio.module.memory.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.module.outline.entity.Outline;
import com.jjxmas.ainovelstudio.module.outline.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.module.project.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.module.version.service.VersionService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private final ProjectMapper projectMapper;
    private final OutlineMapper outlineMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;
    private final ChapterMemoryService chapterMemoryService;

    public ChapterServiceImpl(
            ProjectMapper projectMapper,
            OutlineMapper outlineMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService,
            ChapterMemoryService chapterMemoryService) {
        this.projectMapper = projectMapper;
        this.outlineMapper = outlineMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.chapterMemoryService = chapterMemoryService;
    }

    @Override
    public List<ChapterResponse> listChapters(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return list(new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getProjectId, projectId)
                        .orderByAsc(Chapter::getChapterNo))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChapterResponse confirmChapterOutline(Long chapterId) {
        Chapter chapter = requireChapter(chapterId);
        requireConfirmedGlobalOutline(chapter.getProjectId());
        chapter.setConfirmedOutlineAt(LocalDateTime.now()).setStatus("content_pending");
        updateById(chapter);
        return toResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse generateChapter(ChapterGenerateRequest request) {
        Chapter chapter = requireChapter(request.getChapterId());
        if (!chapter.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "章节不属于当前作品");
        }
        requireConfirmedGlobalOutline(chapter.getProjectId());

        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? chapter.getTitle()
                : request.getTitle();
        String outline = request.getOutline() == null || request.getOutline().isBlank()
                ? chapter.getOutline()
                : request.getOutline();
        Map<String, Object> context = chapterMemoryService.buildChapterContext(chapter);
        AiGenerateResult result = aiOrchestratorService.generateChapter(
                request.getModelConfigId(),
                context,
                title,
                outline,
                request.getRevisionAdvice());
        String content = result.getContent();
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
                Map.of("title", title, "outline", outline, "context", context),
                Map.of("chapter", snapshot, "modelName", blankToEmpty(result.getModelName()), "usage", result.getUsage() == null ? Map.of() : result.getUsage()));
        versionService.recordVersion(chapter.getProjectId(), "chapter", chapter.getId(), snapshot, "ai_generate", "AI 生成章节正文", request.getModelConfigId(), jobId);
        chapterMemoryService.refreshAfterChapterContent(chapter, request.getModelConfigId());
        return toResponse(chapter);
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
                request.getChangeNote() == null ? "用户直接修改章节正文" : request.getChangeNote(),
                null,
                null);
        chapterMemoryService.refreshAfterChapterContent(chapter, null);
        return toResponse(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse rewriteChapter(Long chapterId, ChapterRewriteRequest request) {
        Chapter chapter = requireChapter(chapterId);
        if (chapter.getContent() == null || chapter.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成章节正文，再提交修改意见重生成");
        }
        Map<String, Object> context = chapterMemoryService.buildChapterContext(chapter);
        AiGenerateResult result = aiOrchestratorService.rewriteChapter(
                request.getModelConfigId(),
                context,
                chapter.getContent(),
                request.getInstruction());
        String content = result.getContent();
        chapter.setContent(content).setWordCount(countWords(content)).setStatus("drafted");
        updateById(chapter);
        Map<String, Object> snapshot = chapterSnapshot(chapter);
        Long jobId = generationJobService.recordFinishedJob(
                chapter.getProjectId(),
                "chapter_rewrite",
                "chapter",
                chapter.getId(),
                request.getModelConfigId(),
                Map.of("instruction", request.getInstruction(), "context", context),
                Map.of("chapter", snapshot, "modelName", blankToEmpty(result.getModelName()), "usage", result.getUsage() == null ? Map.of() : result.getUsage()));
        versionService.recordVersion(chapter.getProjectId(), "chapter", chapter.getId(), snapshot, "ai_rewrite", "根据用户修改意见重生成章节正文", request.getModelConfigId(), jobId);
        chapterMemoryService.refreshAfterChapterContent(chapter, request.getModelConfigId());
        return toResponse(chapter);
    }

    private Chapter requireChapter(Long chapterId) {
        Chapter chapter = getById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    private void requireConfirmedGlobalOutline(Long projectId) {
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .isNotNull(Outline::getConfirmedAt)
                .last("LIMIT 1"));
        if (outline == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认全局大纲，再进入章节阶段");
        }
    }

    private ChapterResponse toResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .chapterNo(chapter.getChapterNo())
                .title(chapter.getTitle())
                .outline(chapter.getOutline())
                .content(chapter.getContent())
                .wordCount(chapter.getWordCount())
                .status(chapter.getStatus())
                .outlineConfirmed(chapter.getConfirmedOutlineAt() != null)
                .build();
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
