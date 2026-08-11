package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContentUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.service.ChapterService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 章节服务实现，负责章节查询、大纲确认、正文生成、编辑和重写。
 */
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private final ProjectMapper projectMapper;
    private final OutlineMapper outlineMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;
    private final ChapterMemoryService chapterMemoryService;
    private final ChapterConverter chapterConverter;

    /**
     * 注入章节流程所需的 Mapper、任务服务、版本服务、AI 编排和记忆服务。
     */
    public ChapterServiceImpl(
            ProjectMapper projectMapper,
            OutlineMapper outlineMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService,
            ChapterMemoryService chapterMemoryService,
            ChapterConverter chapterConverter) {
        this.projectMapper = projectMapper;
        this.outlineMapper = outlineMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.chapterMemoryService = chapterMemoryService;
        this.chapterConverter = chapterConverter;
    }

    /**
     * 查询指定项目下的章节列表。
     */
    @Override
    public List<ChapterResponse> listChapters(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return chapterConverter.toResponseList(list(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByAsc(Chapter::getChapterNo)));
    }

    /**
     * 确认章节大纲并将章节状态推进到待生成正文。
     */
    @Override
    @Transactional
    public void confirmChapterOutline(Long chapterId) {
        Chapter chapter = requireChapter(chapterId);
        requireConfirmedGlobalOutline(chapter.getProjectId());
        chapter.setConfirmedOutlineAt(LocalDateTime.now()).setStatus("content_pending");
        updateById(chapter);
    }

    /**
     * 调用 AI 生成章节正文，记录任务、版本并刷新章节记忆。
     */
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
        return chapterConverter.toResponse(chapter);
    }

    /**
     * 手动更新章节正文，记录版本并刷新章节记忆。
     */
    @Override
    @Transactional
    public void updateChapterContent(Long chapterId, ChapterContentUpdateRequest request) {
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
    }

    /**
     * 根据修改指令调用 AI 重写章节正文。
     */
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
        return chapterConverter.toResponse(chapter);
    }

    /**
     * 获取章节实体，不存在时抛出业务异常。
     */
    private Chapter requireChapter(Long chapterId) {
        Chapter chapter = getById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    /**
     * 校验项目是否已有确认后的全局大纲。
     */
    private void requireConfirmedGlobalOutline(Long projectId) {
        Outline outline = outlineMapper.selectOne(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .isNotNull(Outline::getConfirmedAt)
                .last("LIMIT 1"));
        if (outline == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认全局大纲，再进入章节阶段");
        }
    }

    /**
     * 将章节实体转换为章节响应对象。
     */
    /**
     * 构造章节版本快照内容。
     */
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

    /**
     * 按去空白后的字符数估算章节字数。
     */
    private int countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.replaceAll("\\s+", "").length();
    }

    /**
     * 将 null 文本转换为空字符串。
     */
    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
