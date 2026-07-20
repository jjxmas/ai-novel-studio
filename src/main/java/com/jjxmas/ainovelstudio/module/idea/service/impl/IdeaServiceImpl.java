package com.jjxmas.ainovelstudio.module.idea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.module.generation.service.GenerationJobService;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.module.idea.dto.IdeaUpdateRequest;
import com.jjxmas.ainovelstudio.module.idea.entity.Idea;
import com.jjxmas.ainovelstudio.module.idea.entity.IdeaEvaluation;
import com.jjxmas.ainovelstudio.module.idea.mapper.IdeaEvaluationMapper;
import com.jjxmas.ainovelstudio.module.idea.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.module.idea.service.IdeaService;
import com.jjxmas.ainovelstudio.module.project.entity.Project;
import com.jjxmas.ainovelstudio.module.project.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.module.version.service.VersionService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdeaServiceImpl extends ServiceImpl<IdeaMapper, Idea> implements IdeaService {

    private final IdeaEvaluationMapper ideaEvaluationMapper;
    private final ProjectMapper projectMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;

    public IdeaServiceImpl(
            IdeaEvaluationMapper ideaEvaluationMapper,
            ProjectMapper projectMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService) {
        this.ideaEvaluationMapper = ideaEvaluationMapper;
        this.projectMapper = projectMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
    }

    @Override
    @Transactional
    public List<IdeaResponse> generateIdeas(IdeaGenerateRequest request) {
        Project project = requireProject(request.getProjectId());
        int ideaCount = normalizeIdeaCount(request.getIdeaCount());
        return IntStream.rangeClosed(1, ideaCount)
                .mapToObj(index -> createGeneratedIdea(project, request, index))
                .toList();
    }

    @Override
    public List<IdeaResponse> listIdeas(Long projectId) {
        requireProject(projectId);
        return list(new LambdaQueryWrapper<Idea>()
                        .eq(Idea::getProjectId, projectId)
                        .orderByDesc(Idea::getSelectedAt)
                        .orderByDesc(Idea::getUpdatedAt))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public IdeaResponse updateIdea(Long ideaId, IdeaUpdateRequest request) {
        Idea idea = requireIdea(ideaId);
        idea.setTitle(request.getTitle())
                .setSellingPoints(JsonUtils.toJson(request.getSellingPoints() == null ? List.of() : request.getSellingPoints()))
                .setWorldview(request.getWorldview())
                .setMainConflict(request.getMainConflict())
                .setEstimatedWordCount(request.getEstimatedWordCount() == null ? idea.getEstimatedWordCount() : request.getEstimatedWordCount())
                .setSummary(request.getSummary());
        updateById(idea);
        versionService.recordVersion(
                idea.getProjectId(),
                "idea",
                idea.getId(),
                ideaSnapshot(idea),
                "user_edit",
                defaultText(request.getChangeNote(), "用户直接修改创意"),
                idea.getModelConfigId(),
                null);
        return toResponse(idea);
    }

    @Override
    @Transactional
    public IdeaResponse rewriteIdea(Long ideaId, IdeaRewriteRequest request) {
        Idea idea = requireIdea(ideaId);
        AiGenerateResult result = aiOrchestratorService.rewriteIdea(
                request.getModelConfigId(),
                ideaSnapshot(idea).toString(),
                request.getInstruction());
        String content = defaultText(result.getContent(), idea.getSummary() + "\n\n根据修改意见调整：" + request.getInstruction());
        idea.setTitle(extractField(content, "标题", defaultText(idea.getTitle(), "重写后的创意方案")))
                .setWorldview(extractField(content, "世界观", defaultText(idea.getWorldview(), "")))
                .setMainConflict(extractField(content, "主线冲突", defaultText(idea.getMainConflict(), "根据修改意见补强主线冲突")))
                .setSummary(content)
                .setModelConfigId(request.getModelConfigId());
        updateById(idea);
        Long jobId = generationJobService.recordFinishedJob(
                idea.getProjectId(),
                "idea_rewrite",
                "idea",
                idea.getId(),
                request.getModelConfigId(),
                Map.of("instruction", request.getInstruction()),
                Map.of("idea", ideaSnapshot(idea), "modelName", defaultText(result.getModelName(), "")));
        versionService.recordVersion(
                idea.getProjectId(),
                "idea",
                idea.getId(),
                ideaSnapshot(idea),
                "ai_rewrite",
                "根据用户修改意见重生成创意",
                request.getModelConfigId(),
                jobId);
        return toResponse(idea);
    }

    @Override
    @Transactional
    public IdeaResponse selectIdea(Long ideaId) {
        Idea idea = requireIdea(ideaId);
        update(new LambdaUpdateWrapper<Idea>()
                .eq(Idea::getProjectId, idea.getProjectId())
                .set(Idea::getStatus, "candidate")
                .set(Idea::getSelectedAt, null));
        idea.setStatus("selected").setSelectedAt(LocalDateTime.now());
        updateById(idea);

        Project project = requireProject(idea.getProjectId());
        project.setSelectedIdeaId(idea.getId()).setStatus("idea_selected");
        projectMapper.updateById(project);
        versionService.recordVersion(
                project.getId(),
                "idea",
                idea.getId(),
                ideaSnapshot(idea),
                "confirm",
                "选定创意方案",
                idea.getModelConfigId(),
                null);
        return toResponse(idea);
    }

    private IdeaResponse createGeneratedIdea(Project project, IdeaGenerateRequest request, int index) {
        String genreText = String.join(" + ", JsonUtils.toStringList(project.getGenres()));
        Map<String, Object> context = ideaContext(project, request, genreText);
        AiGenerateResult result = aiOrchestratorService.generateIdea(request.getModelConfigId(), context, index);
        String content = defaultText(result.getContent(), request.getBriefDescription() + "。方案" + index + " 强调长篇连载节奏和阶段性目标");
        Idea idea = new Idea()
                .setProjectId(project.getId())
                .setTitle(firstTitle(content, "创意方案 " + index + "：" + genreText + "新手长篇"))
                .setSellingPoints(JsonUtils.toJson(List.of("开局目标清晰", "升级反馈稳定", "长期冲突可扩展")))
                .setWorldview(extractField(content, "世界观", "以《" + genreText + "》为底色，主角从熟悉环境进入更大的规则体系"))
                .setMainConflict(extractField(content, "主线冲突", "主角想掌控命运，但既有秩序不断压缩选择空间"))
                .setEstimatedWordCount(project.getTargetWordCountMax() == null || project.getTargetWordCountMax() == 0
                        ? 2_000_000
                        : project.getTargetWordCountMax())
                .setSummary(content)
                .setStatus("candidate")
                .setModelConfigId(request.getModelConfigId());
        save(idea);

        IdeaEvaluation evaluation = new IdeaEvaluation()
                .setIdeaId(idea.getId())
                .setRoundNo(1)
                .setLongFormPotentialScore(82.0 + index)
                .setConflictScore(78.0 + index)
                .setNoveltyScore(70.0 + index)
                .setBeginnerFriendlinessScore(86.0)
                .setPlatformFitScore(80.0)
                .setRiskLevel("medium")
                .setStrengths(JsonUtils.toJson(List.of("主线目标容易展开", "人物成长线清晰")))
                .setRisks(JsonUtils.toJson(List.of("需要避免套路化表达", "中后期需要持续制造新矛盾")))
                .setSuggestions(JsonUtils.toJson(List.of("尽早设计阶段性反转", "每卷保留一个可回收伏笔")))
                .setOverallComment("适合作为新手长篇起点，但需要在设定库阶段固定规则边界")
                .setModelConfigId(request.getModelConfigId());
        ideaEvaluationMapper.insert(evaluation);

        Map<String, Object> snapshot = ideaSnapshot(idea);
        Long jobId = generationJobService.recordFinishedJob(
                project.getId(),
                "idea_generation",
                "idea",
                idea.getId(),
                request.getModelConfigId(),
                Map.of("context", context, "ideaCount", normalizeIdeaCount(request.getIdeaCount()), "index", index),
                Map.of("idea", snapshot, "modelName", defaultText(result.getModelName(), "")));
        versionService.recordVersion(project.getId(), "idea", idea.getId(), snapshot, "ai_generate", "AI 生成创意", request.getModelConfigId(), jobId);
        return toResponse(idea, evaluation);
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    private Idea requireIdea(Long ideaId) {
        Idea idea = getById(ideaId);
        if (idea == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创意不存在");
        }
        return idea;
    }

    private IdeaResponse toResponse(Idea idea) {
        IdeaEvaluation evaluation = ideaEvaluationMapper.selectOne(new LambdaQueryWrapper<IdeaEvaluation>()
                .eq(IdeaEvaluation::getIdeaId, idea.getId())
                .orderByDesc(IdeaEvaluation::getRoundNo)
                .last("LIMIT 1"));
        return toResponse(idea, evaluation);
    }

    private IdeaResponse toResponse(Idea idea, IdeaEvaluation evaluation) {
        return IdeaResponse.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .sellingPoints(JsonUtils.toStringList(idea.getSellingPoints()))
                .worldview(idea.getWorldview())
                .mainConflict(idea.getMainConflict())
                .estimatedWordCount(idea.getEstimatedWordCount())
                .summary(idea.getSummary())
                .longFormPotentialScore(score(evaluation == null ? null : evaluation.getLongFormPotentialScore()))
                .conflictScore(score(evaluation == null ? null : evaluation.getConflictScore()))
                .noveltyScore(score(evaluation == null ? null : evaluation.getNoveltyScore()))
                .beginnerFriendlinessScore(score(evaluation == null ? null : evaluation.getBeginnerFriendlinessScore()))
                .platformFitScore(score(evaluation == null ? null : evaluation.getPlatformFitScore()))
                .riskLevel(evaluation == null ? null : evaluation.getRiskLevel())
                .status(idea.getStatus())
                .build();
    }

    private Map<String, Object> ideaSnapshot(Idea idea) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", defaultText(idea.getTitle(), ""));
        snapshot.put("sellingPoints", JsonUtils.toStringList(idea.getSellingPoints()));
        snapshot.put("worldview", defaultText(idea.getWorldview(), ""));
        snapshot.put("mainConflict", defaultText(idea.getMainConflict(), ""));
        snapshot.put("estimatedWordCount", idea.getEstimatedWordCount() == null ? 0 : idea.getEstimatedWordCount());
        snapshot.put("summary", defaultText(idea.getSummary(), ""));
        snapshot.put("status", defaultText(idea.getStatus(), ""));
        return snapshot;
    }

    private Map<String, Object> ideaContext(Project project, IdeaGenerateRequest request, String genreText) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("作品标题", defaultText(project.getTitle(), ""));
        context.put("题材", genreText);
        context.put("平台目标", defaultText(project.getPlatformTarget(), ""));
        context.put("目标字数下限", project.getTargetWordCountMin() == null ? 0 : project.getTargetWordCountMin());
        context.put("目标字数上限", project.getTargetWordCountMax() == null ? 0 : project.getTargetWordCountMax());
        context.put("风格偏好", defaultText(project.getStylePreference(), ""));
        context.put("作品描述", defaultText(request.getBriefDescription(), project.getProjectBrief()));
        return context;
    }

    private String firstTitle(String content, String fallback) {
        String title = extractField(content, "标题", "");
        if (!title.isBlank()) {
            return title;
        }
        return firstLine(content, fallback);
    }

    private String extractField(String content, String label, String fallback) {
        if (content == null || content.isBlank()) {
            return fallback;
        }
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(label + "：") || trimmed.startsWith(label + ":")) {
                return trimmed.substring(trimmed.indexOf(':') >= 0 ? trimmed.indexOf(':') + 1 : trimmed.indexOf('：') + 1).trim();
            }
        }
        return fallback;
    }

    private String firstLine(String content, String fallback) {
        if (content == null || content.isBlank()) {
            return fallback;
        }
        return content.lines()
                .map(String::trim)
                .filter((line) -> !line.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private int normalizeIdeaCount(Integer ideaCount) {
        if (ideaCount == null) {
            return 3;
        }
        return Math.max(1, Math.min(ideaCount, 5));
    }

    private Integer score(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
