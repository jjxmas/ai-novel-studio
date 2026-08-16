package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.CheckResultMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.CheckIssueResponse;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.CheckResult;
import com.jjxmas.ainovelstudio.service.CheckService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 执行章节质量检查，并在正文版本未变化时保存权威检查结果。
 */
@Service
public class CheckServiceImpl extends ServiceImpl<CheckResultMapper, CheckResult> implements CheckService {

    private static final Logger log = LoggerFactory.getLogger(CheckServiceImpl.class);
    private static final int PREVIOUS_TAIL_LENGTH = 1200;

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final GenerationJobService generationJobService;
    private final AiOrchestratorService aiOrchestratorService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public CheckServiceImpl(
            ProjectMapper projectMapper,
            ChapterMapper chapterMapper,
            GenerationJobService generationJobService,
            AiOrchestratorService aiOrchestratorService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.generationJobService = generationJobService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CheckResponse runCheck(CheckRequest request) {
        String checkType = normalizeCheckType(request.getCheckType());
        List<ChapterCheckInput> inputs = loadInputs(request);
        Map<String, Object> jobInput = jobInput(request, checkType, inputs.size());
        List<ChapterCheckReport> reports;
        try {
            reports = inputs.stream()
                    .map(input -> checkChapter(request.getModelConfigId(), checkType, input))
                    .toList();
        } catch (RuntimeException exception) {
            recordFailedJob(request, jobInput, exception);
            throw exception;
        }
        return transactionTemplate.execute(status -> persist(request, checkType, reports, jobInput));
    }

    private List<ChapterCheckInput> loadInputs(CheckRequest request) {
        if (projectMapper.selectById(request.getProjectId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        if (request.getChapterId() != null) {
            Chapter chapter = chapterMapper.selectById(request.getChapterId());
            requireCheckableChapter(request.getProjectId(), chapter);
            if (request.getTargetText() != null
                    && !request.getTargetText().isBlank()
                    && !request.getTargetText().equals(chapter.getContent())) {
                throw new BusinessException(ErrorCode.PARAMETER_ERROR, "检查文本必须与已保存的章节正文一致");
            }
            return List.of(toInput(chapter, previousChapter(chapter)));
        }

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, request.getProjectId())
                .orderByAsc(Chapter::getChapterNo));
        List<ChapterCheckInput> inputs = new ArrayList<>();
        Chapter previous = null;
        for (Chapter chapter : chapters) {
            if (hasContent(chapter)) {
                inputs.add(toInput(chapter, previous));
                previous = chapter;
            }
        }
        if (inputs.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成至少一章正文，再进行检查");
        }
        return inputs;
    }

    private Chapter previousChapter(Chapter chapter) {
        if (chapter.getChapterNo() == null) {
            return null;
        }
        return chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, chapter.getProjectId())
                .lt(Chapter::getChapterNo, chapter.getChapterNo())
                .isNotNull(Chapter::getContent)
                .ne(Chapter::getContent, "")
                .orderByDesc(Chapter::getChapterNo)
                .last("LIMIT 1"));
    }

    private ChapterCheckReport checkChapter(Long modelConfigId, String checkType, ChapterCheckInput input) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("chapterNo", input.chapterNo());
        context.put("title", blank(input.title()));
        context.put("outline", blank(input.outline()));
        context.put("content", input.content());
        context.put("previousChapterNo", input.previousChapterNo() == null ? "无" : input.previousChapterNo());
        context.put("previousTitle", blank(input.previousTitle()));
        context.put("previousTail", blank(input.previousTail()));
        AiGenerateResult result = aiOrchestratorService.checkChapter(modelConfigId, checkType, context);
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())
                || result.getContent() == null || result.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "AI_CHECK_EMPTY_RESPONSE");
        }
        return parseReport(input, result);
    }

    private ChapterCheckReport parseReport(ChapterCheckInput input, AiGenerateResult result) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(result.getContent()));
            JsonNode issueNodes = root.get("issues");
            if (!root.isObject() || issueNodes == null || !issueNodes.isArray()) {
                throw invalidAiResponse();
            }
            List<CheckIssueResponse> issues = new ArrayList<>();
            for (JsonNode issueNode : issueNodes) {
                issues.add(CheckIssueResponse.builder()
                        .type(requiredText(issueNode, "type"))
                        .severity(normalizeSeverity(requiredText(issueNode, "severity")))
                        .description(requiredText(issueNode, "description"))
                        .suggestion(requiredText(issueNode, "suggestion"))
                        .reference(issueReference(input, issueNode))
                        .build());
            }
            return new ChapterCheckReport(
                    input,
                    optionalText(root, "summary", "检查完成"),
                    List.copyOf(issues),
                    result.getModelName());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidAiResponse();
        }
    }

    private CheckResponse persist(
            CheckRequest request,
            String checkType,
            List<ChapterCheckReport> reports,
            Map<String, Object> jobInput) {
        List<Chapter> lockedChapters = new ArrayList<>();
        for (ChapterCheckReport report : reports) {
            Chapter current = chapterMapper.selectByIdForUpdate(report.input().chapterId());
            if (!matchesSnapshot(current, report.input())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "检查期间章节正文已被修改，请重新检查");
            }
            lockedChapters.add(current);
        }

        List<CheckIssueResponse> allIssues = reports.stream()
                .flatMap(report -> report.issues().stream())
                .toList();
        Long jobId = generationJobService.recordFinishedJob(
                request.getProjectId(),
                "quality_check",
                request.getChapterId() == null ? "project" : "chapter",
                request.getChapterId(),
                request.getModelConfigId(),
                jobInput,
                Map.of(
                        "chapterCount", reports.size(),
                        "issueCount", allIssues.size(),
                        "models", reports.stream()
                                .map(ChapterCheckReport::modelName)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()));

        LocalDateTime checkedAt = LocalDateTime.now();
        if (request.getChapterId() == null) {
            resolvePreviousProjectResults(request.getProjectId(), checkedAt);
        }
        for (int index = 0; index < reports.size(); index++) {
            ChapterCheckReport report = reports.get(index);
            resolvePreviousChapterResults(request.getProjectId(), report.input().chapterId(), checkType, checkedAt);
            for (CheckIssueResponse issue : report.issues()) {
                save(new CheckResult()
                        .setProjectId(request.getProjectId())
                        .setChapterId(report.input().chapterId())
                        .setJobId(jobId)
                        .setCheckType(checkType + ":" + issue.getType())
                        .setSeverity(issue.getSeverity())
                        .setTargetType("chapter")
                        .setTargetId(report.input().chapterId())
                        .setIssue(issue.getDescription())
                        .setSuggestion(issue.getSuggestion()));
            }
            Chapter lockedChapter = lockedChapters.get(index);
            lockedChapter.setCheckedAt(checkedAt);
            chapterMapper.updateById(lockedChapter);
        }

        String summary = reports.size() == 1
                ? reports.get(0).summary()
                : "已完成 " + reports.size() + " 章检查，共发现 " + allIssues.size() + " 项问题。";
        return CheckResponse.builder()
                .issueCount(allIssues.size())
                .issues(allIssues)
                .summary(summary)
                .build();
    }

    private void resolvePreviousChapterResults(
            Long projectId,
            Long chapterId,
            String checkType,
            LocalDateTime resolvedAt) {
        LambdaUpdateWrapper<CheckResult> wrapper = new LambdaUpdateWrapper<CheckResult>()
                .eq(CheckResult::getProjectId, projectId)
                .eq(CheckResult::getChapterId, chapterId)
                .isNull(CheckResult::getResolvedAt);
        if (!"all".equals(checkType)) {
            wrapper.and(condition -> condition
                    .eq(CheckResult::getCheckType, checkType)
                    .or()
                    .likeRight(CheckResult::getCheckType, checkType + ":"));
        }
        wrapper.set(CheckResult::getResolvedAt, resolvedAt);
        getBaseMapper().update(null, wrapper);
    }

    private void resolvePreviousProjectResults(Long projectId, LocalDateTime resolvedAt) {
        getBaseMapper().update(null, new LambdaUpdateWrapper<CheckResult>()
                .eq(CheckResult::getProjectId, projectId)
                .eq(CheckResult::getTargetType, "project")
                .isNull(CheckResult::getResolvedAt)
                .set(CheckResult::getResolvedAt, resolvedAt));
    }

    private Map<String, Object> jobInput(CheckRequest request, String checkType, int chapterCount) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("checkType", checkType);
        input.put("chapterId", request.getChapterId());
        input.put("chapterCount", chapterCount);
        return input;
    }

    private void recordFailedJob(CheckRequest request, Map<String, Object> input, RuntimeException exception) {
        try {
            generationJobService.recordFailedJob(
                    request.getProjectId(),
                    "quality_check",
                    request.getChapterId() == null ? "project" : "chapter",
                    request.getChapterId(),
                    request.getModelConfigId(),
                    input,
                    exception.getMessage());
        } catch (RuntimeException recordException) {
            log.warn("Failed to record quality check failure. projectId={}", request.getProjectId(), recordException);
        }
    }

    private ChapterCheckInput toInput(Chapter chapter, Chapter previous) {
        return new ChapterCheckInput(
                chapter.getId(),
                chapter.getProjectId(),
                chapter.getChapterNo(),
                chapter.getTitle(),
                chapter.getOutline(),
                chapter.getContent(),
                chapter.getLastContentVersionNo(),
                previous == null ? null : previous.getChapterNo(),
                previous == null ? null : previous.getTitle(),
                previous == null ? null : tail(previous.getContent()));
    }

    private boolean matchesSnapshot(Chapter chapter, ChapterCheckInput input) {
        return chapter != null
                && Objects.equals(chapter.getProjectId(), input.projectId())
                && Objects.equals(chapter.getChapterNo(), input.chapterNo())
                && Objects.equals(chapter.getTitle(), input.title())
                && Objects.equals(chapter.getOutline(), input.outline())
                && Objects.equals(chapter.getContent(), input.content())
                && Objects.equals(chapter.getLastContentVersionNo(), input.contentVersionNo());
    }

    private void requireCheckableChapter(Long projectId, Chapter chapter) {
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        if (!hasContent(chapter)) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成章节正文，再进行检查");
        }
    }

    private boolean hasContent(Chapter chapter) {
        return chapter.getContent() != null && !chapter.getContent().isBlank();
    }

    private String normalizeCheckType(String checkType) {
        String normalized = checkType == null ? "" : checkType.trim().toLowerCase();
        if (!List.of("all", "continuity", "style").contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "检查类型只支持 all、continuity 或 style");
        }
        return normalized;
    }

    private String normalizeSeverity(String severity) {
        String normalized = severity.trim().toLowerCase();
        if (!List.of("high", "medium", "low").contains(normalized)) {
            throw invalidAiResponse();
        }
        return normalized;
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw invalidAiResponse();
        }
        return value.asText().trim();
    }

    private String optionalText(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.get(fieldName);
        return value == null || !value.isTextual() || value.asText().isBlank()
                ? defaultValue
                : value.asText().trim();
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw invalidAiResponse();
        }
        return content.substring(start, end + 1);
    }

    private String chapterReference(ChapterCheckInput input) {
        return "第" + input.chapterNo() + "章《" + blank(input.title()) + "》";
    }

    private String issueReference(ChapterCheckInput input, JsonNode issueNode) {
        String chapterReference = chapterReference(input);
        String detail = optionalText(issueNode, "reference", "");
        return detail.isBlank() ? chapterReference : chapterReference + "：" + detail;
    }

    private String tail(String content) {
        if (content == null || content.length() <= PREVIOUS_TAIL_LENGTH) {
            return blank(content);
        }
        return content.substring(content.length() - PREVIOUS_TAIL_LENGTH);
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }

    private BusinessException invalidAiResponse() {
        return new BusinessException(ErrorCode.AI_TASK_UNAVAILABLE, "AI_CHECK_RESPONSE_INVALID");
    }

    private record ChapterCheckInput(
            Long chapterId,
            Long projectId,
            Integer chapterNo,
            String title,
            String outline,
            String content,
            Integer contentVersionNo,
            Integer previousChapterNo,
            String previousTitle,
            String previousTail) {
    }

    private record ChapterCheckReport(
            ChapterCheckInput input,
            String summary,
            List<CheckIssueResponse> issues,
            String modelName) {
    }
}
