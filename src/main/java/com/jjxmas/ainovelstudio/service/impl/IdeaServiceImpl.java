package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.IdeaEvaluation;
import com.jjxmas.ainovelstudio.mapper.IdeaEvaluationMapper;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.service.IdeaService;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 创意服务实现，负责创意生成、编辑、重写、选中和版本记录。
 */
@Service
@Slf4j
public class IdeaServiceImpl extends ServiceImpl<IdeaMapper, Idea> implements IdeaService {

    private final IdeaEvaluationMapper ideaEvaluationMapper;
    private final ProjectMapper projectMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final AiOrchestratorService aiOrchestratorService;
    private final ModelConfigMapper modelConfigMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 注入创意流程所需的 Mapper、任务服务、版本服务和 AI 编排服务。
     */
    public IdeaServiceImpl(
            IdeaEvaluationMapper ideaEvaluationMapper,
            ProjectMapper projectMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            AiOrchestratorService aiOrchestratorService, ModelConfigMapper modelConfigMapper,
            TransactionTemplate transactionTemplate) {
        this.ideaEvaluationMapper = ideaEvaluationMapper;
        this.projectMapper = projectMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.modelConfigMapper = modelConfigMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 为项目批量生成创意候选方案。
     */
    @Override
    public List<IdeaResponse> generateIdeas(IdeaGenerateRequest request) {
        Project project = requireProject(request.getProjectId());
        int targetGenerateNum = normalizeIdeaCount(request.getIdeaCount());
        if (targetGenerateNum <= 0) {
            return List.of();
        }

//        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
//            List<CompletableFuture<IdeaResponse>> futureList = IntStream.range(0, targetGenerateNum)
//                    .mapToObj(taskIndex -> CompletableFuture.supplyAsync(
//                            () -> transactionTemplate.execute(status -> createGeneratedIdea(project, request)),
//                            virtualExecutor
//                    ).exceptionally(ex -> {
//                        // 利用下标精准打印第几条大模型生成失败，方便排查接口限流、超时
//                        log.error("批量生成创意：第{}条大模型调用异常", ex.getCause());
//                        return null;
//                    }))
//                    .toList();
//
//            // 过滤失败为空的数据，只返回大模型调用成功的结果
//            return futureList.stream()
//                    .map(CompletableFuture::join)
//                    .filter(Objects::nonNull)
//                    .toList();
//        }
        // 🔴 限制最大并发数量，防止一次性创建几百上千虚拟线程打崩大模型接口
        // 最大并发20
        Semaphore semaphore = new Semaphore(20);

        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<IdeaResponse>> futureList = new ArrayList<>();

            for (int taskIndex = 0; taskIndex < targetGenerateNum; taskIndex++) {
                final int idx = taskIndex;
                // 提交前先获取许可，控制并发，不会出现许可泄漏
                semaphore.acquireUninterruptibly();
                CompletableFuture<IdeaResponse> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                return createGeneratedIdea(project, request);
                            } finally {
                                semaphore.release();
                            }
                        }, virtualExecutor)
                        .orTimeout(300, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            Throwable realEx = ex.getCause() != null ? ex.getCause() : ex;
                            log.error("批量生成创意：第{}条大模型调用异常", idx,realEx);
                            return null;
                        });

                futureList.add(future);
            }
            CompletableFuture.allOf(futureList.toArray(CompletableFuture[]::new)).join();
            return futureList.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    /**
     * 查询项目创意列表，选中和最近更新的创意优先。
     */
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

    /**
     * 手动更新创意内容并记录用户编辑版本。
     */
    @Override
    @Transactional
    public IdeaResponse updateIdea(Long ideaId, IdeaUpdateRequest request) {
        Idea idea = requireIdea(ideaId);
        idea.setTitle(request.getTitle())
                .setSellingPoints(request.getSellingPoints() == null ? List.of() : request.getSellingPoints())
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

    /**
     * 根据用户指令调用 AI 重写指定创意。
     */
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

    /**
     * 将创意设为项目选中方案，并重置同项目其他候选状态。
     */
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

    @Override
    @Transactional
    public void deleteIdea(Long ideaId) {
        Idea idea = requireIdea(ideaId);
        if ("selected".equals(idea.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已选中的创意不能删除，请先选择其他创意");
        }
        removeById(ideaId);
        versionService.recordVersion(
                idea.getProjectId(),
                "idea",
                idea.getId(),
                ideaSnapshot(idea),
                "delete",
                "删除创意方案",
                idea.getModelConfigId(),
                null);
    }

    /**
     * 生成单个创意候选并保存评估、任务和版本记录。
     */
    private IdeaResponse createGeneratedIdea(Project project, IdeaGenerateRequest request) {
        String genreText = String.join(" + ", JsonUtils.toStringList(project.getGenres()));
        Map<String, Object> context = ideaContext(project, request, genreText);

        ModelConfig config = modelConfigMapper.selectOne(
                new QueryWrapper<ModelConfig>()
                        .eq("usage_type", request.getModelType())
                        .eq("enabled", 1)
                        .last("LIMIT 1")
        );
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "可用模型配置不存在");
        }

        AiGenerateResult ideaResult = aiOrchestratorService.generateIdea(config.getId(), context);
        if (ideaResult == null || ideaResult.getContent() == null || ideaResult.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大模型创意响应为空");
        }

        Map<String, Object> ideaMap = JsonUtils.toMap(ideaResult.getContent());
        if (ideaMap == null || ideaMap.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "大模型创意内容 JSON 解析失败");
        }

        Idea aiIdea = new Idea()
                .setProjectId(project.getId())
                .setTitle(textValue(ideaMap, "title", "未命名创意"))
                .setSellingPoints((List<String>) ideaMap.get("sellingPoints"))
                .setWorldview(textValue(ideaMap, "worldview", ""))
                .setMainConflict(textValue(ideaMap, "mainConflict", ""))
                .setEstimatedWordCount(intValue(ideaMap, "estimatedWordCount", defaultEstimatedWordCount(project)))
                .setSummary(textValue(ideaMap, "summary", ""))
                .setStatus("candidate")
                .setModelConfigId(config.getId());
        save(aiIdea);
        if (aiIdea.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创意保存成功但 ID 未回填");
        }

        AiGenerateResult evaluationResult = aiOrchestratorService.evaluateIdea(config.getId(), context, ideaMap);
        if (evaluationResult == null || evaluationResult.getContent() == null || evaluationResult.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大模型评估响应为空");
        }

        Map<String, Object> evaluationMap = JsonUtils.toMap(evaluationResult.getContent());
        if (evaluationMap == null || evaluationMap.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "大模型评估内容 JSON 解析失败");
        }

        IdeaEvaluation ideaEvaluation = new IdeaEvaluation()
                .setIdeaId(aiIdea.getId())
                .setRoundNo(1)
                .setLongFormPotentialScore(doubleValue(evaluationMap, "longFormPotentialScore", 0.0))
                .setConflictScore(doubleValue(evaluationMap, "conflictScore", 0.0))
                .setNoveltyScore(doubleValue(evaluationMap, "noveltyScore", 0.0))
                .setBeginnerFriendlinessScore(doubleValue(evaluationMap, "beginnerFriendlinessScore", 0.0))
                .setPlatformFitScore(doubleValue(evaluationMap, "platformFitScore", 0.0))
                .setRiskLevel(textValue(evaluationMap, "riskLevel", "medium"))
                .setStrengths((List<String>) evaluationMap.get("strengths"))
                .setRisks((List<String>) evaluationMap.get("risks"))
                .setSuggestions((List<String>) evaluationMap.get("suggestions"))
                .setOverallComment(textValue(evaluationMap, "overallComment", ""))
                .setModelConfigId(config.getId());
        ideaEvaluationMapper.insert(ideaEvaluation);

        Map<String, Object> snapshot = ideaSnapshot(aiIdea);
        Long jobId = generationJobService.recordFinishedJob(
                project.getId(),
                "idea_generation",
                "idea",
                aiIdea.getId(),
                config.getId(),
                Map.of("context", context),
                Map.of(
                        "idea", snapshot,
                        "evaluation", evaluationMap,
                        "modelName", defaultText(ideaResult.getModelName(), "")
                ));

        versionService.recordVersion(
                project.getId(),
                "idea",
                aiIdea.getId(),
                snapshot,
                "ai_generate",
                "AI 生成创意",
                config.getId(),
                jobId);

        return toResponse(aiIdea, ideaEvaluation);
    }
    private String textValue(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    private Integer intValue(Map<String, Object> map, String key, Integer fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }

    private Double doubleValue(Map<String, Object> map, String key, Double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return fallback;
    }

    private Integer defaultEstimatedWordCount(Project project) {
        return project.getTargetWordCountMax() == null || project.getTargetWordCountMax() == 0
                ? 2_000_000
                : project.getTargetWordCountMax();
    }


    /**
     * 获取项目实体，不存在时抛出业务异常。
     */
    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    /**
     * 获取创意实体，不存在时抛出业务异常。
     */
    private Idea requireIdea(Long ideaId) {
        Idea idea = getById(ideaId);
        if (idea == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创意不存在");
        }
        return idea;
    }

    /**
     * 查询最新评估后将创意转换为响应对象。
     */
    private IdeaResponse toResponse(Idea idea) {
        IdeaEvaluation evaluation = ideaEvaluationMapper.selectOne(new LambdaQueryWrapper<IdeaEvaluation>()
                .eq(IdeaEvaluation::getIdeaId, idea.getId())
                .orderByDesc(IdeaEvaluation::getRoundNo)
                .last("LIMIT 1"));
        return toResponse(idea, evaluation);
    }

    /**
     * 使用给定评估数据将创意转换为响应对象。
     */
    private IdeaResponse toResponse(Idea idea, IdeaEvaluation evaluation) {
        return IdeaResponse.builder()
                .id(idea.getId())
                .title(idea.getTitle())
                .sellingPoints(idea.getSellingPoints())
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
                .strengths(evaluation == null ? List.of() : defaultList(evaluation.getStrengths()))
                .risks(evaluation == null ? List.of() : defaultList(evaluation.getRisks()))
                .suggestions(evaluation == null ? List.of() : defaultList(evaluation.getSuggestions()))
                .overallComment(evaluation == null ? "" : defaultText(evaluation.getOverallComment(), ""))
                .status(idea.getStatus())
                .build();
    }

    /**
     * 构造创意版本快照内容。
     */
    private Map<String, Object> ideaSnapshot(Idea idea) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", defaultText(idea.getTitle(), ""));
        snapshot.put("sellingPoints", idea.getSellingPoints());
        snapshot.put("worldview", defaultText(idea.getWorldview(), ""));
        snapshot.put("mainConflict", defaultText(idea.getMainConflict(), ""));
        snapshot.put("estimatedWordCount", idea.getEstimatedWordCount() == null ? 0 : idea.getEstimatedWordCount());
        snapshot.put("summary", defaultText(idea.getSummary(), ""));
        snapshot.put("status", defaultText(idea.getStatus(), ""));
        return snapshot;
    }

    /**
     * 构造创意生成时传给 AI 的项目上下文。
     */
    private Map<String, Object> ideaContext(Project project, IdeaGenerateRequest request, String genreText) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("作品标题", defaultText(project.getTitle(), ""));
        context.put("题材", genreText);
        context.put("平台目标", defaultText(project.getPlatformTarget(), ""));
        context.put("目标字数下限", project.getTargetWordCountMin() == null ? 0 : project.getTargetWordCountMin());
        context.put("目标字数上限", project.getTargetWordCountMax() == null ? 0 : project.getTargetWordCountMax());
        context.put("单章目标字数", project.getTargetChapterWordCount() == null ? 3000 : project.getTargetChapterWordCount());
        context.put("风格偏好", defaultText(project.getStylePreference(), ""));
        context.put("作品描述", defaultText( project.getProjectBrief(),""));
        context.put("补充创意方向",defaultText(request.getBriefDescription(),""));
        return context;
    }

    /**
     * 优先提取标题字段，失败时返回正文首行。
     */
    private String firstTitle(String content, String fallback) {
        String title = extractField(content, "标题", "");
        if (!title.isBlank()) {
            return title;
        }
        return firstLine(content, fallback);
    }

    /**
     * 从 AI 文本中按标签提取字段值。
     */
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

    /**
     * 返回文本中的第一个非空行。
     */
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

    /**
     * 将创意生成数量限制在 1 到 5 个之间。
     */
    private int normalizeIdeaCount(Integer ideaCount) {
        if (ideaCount == null) {
            return 3;
        }
        return Math.max(1, Math.min(ideaCount, 5));
    }

    /**
     * 将浮点评分四舍五入为整数评分。
     */
    private Integer score(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    /**
     * 为空白文本提供默认值。
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
