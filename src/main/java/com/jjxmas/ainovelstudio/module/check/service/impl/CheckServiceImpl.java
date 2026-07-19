package com.jjxmas.ainovelstudio.module.check.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.module.chapter.entity.Chapter;
import com.jjxmas.ainovelstudio.module.chapter.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.module.check.dto.CheckIssueResponse;
import com.jjxmas.ainovelstudio.module.check.dto.CheckRequest;
import com.jjxmas.ainovelstudio.module.check.dto.CheckResponse;
import com.jjxmas.ainovelstudio.module.check.entity.CheckResult;
import com.jjxmas.ainovelstudio.module.check.mapper.CheckResultMapper;
import com.jjxmas.ainovelstudio.module.check.service.CheckService;
import com.jjxmas.ainovelstudio.module.generation.service.GenerationJobService;
import com.jjxmas.ainovelstudio.module.project.mapper.ProjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckServiceImpl extends ServiceImpl<CheckResultMapper, CheckResult> implements CheckService {

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final GenerationJobService generationJobService;

    public CheckServiceImpl(ProjectMapper projectMapper, ChapterMapper chapterMapper, GenerationJobService generationJobService) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.generationJobService = generationJobService;
    }

    @Override
    @Transactional
    public CheckResponse runCheck(CheckRequest request) {
        if (projectMapper.selectById(request.getProjectId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        Chapter chapter = null;
        List<Chapter> chaptersToMarkChecked = new ArrayList<>();
        if (request.getChapterId() != null) {
            chapter = chapterMapper.selectById(request.getChapterId());
            if (chapter == null || !request.getProjectId().equals(chapter.getProjectId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "章节不存在");
            }
            if (chapter.getContent() == null || chapter.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成章节正文，再进行检查");
            }
            chaptersToMarkChecked.add(chapter);
        } else {
            List<Chapter> contentReadyChapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                    .eq(Chapter::getProjectId, request.getProjectId())
                    .isNotNull(Chapter::getContent));
            if (contentReadyChapters.stream().noneMatch(item -> item.getContent() != null && !item.getContent().isBlank())) {
                throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成至少一章正文，再进行检查");
            }
            chaptersToMarkChecked.addAll(contentReadyChapters.stream()
                    .filter(item -> item.getContent() != null && !item.getContent().isBlank())
                    .toList());
        }

        List<CheckIssueResponse> issues = mockIssues(request);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("checkType", request.getCheckType());
        input.put("chapterId", request.getChapterId());
        input.put("targetTextLength", request.getTargetText() == null ? 0 : request.getTargetText().length());
        Long jobId = generationJobService.recordFinishedJob(
                request.getProjectId(),
                "quality_check",
                request.getChapterId() == null ? "project" : "chapter",
                request.getChapterId(),
                null,
                input,
                Map.of("issueCount", issues.size()));

        for (CheckIssueResponse issue : issues) {
            CheckResult result = new CheckResult()
                    .setProjectId(request.getProjectId())
                    .setChapterId(request.getChapterId())
                    .setJobId(jobId)
                    .setCheckType(issue.getType())
                    .setSeverity(issue.getSeverity())
                    .setTargetType(request.getChapterId() == null ? "project" : "chapter")
                    .setTargetId(request.getChapterId())
                    .setIssue(issue.getDescription())
                    .setSuggestion(issue.getSuggestion());
            save(result);
        }

        LocalDateTime checkedAt = LocalDateTime.now();
        for (Chapter checkedChapter : chaptersToMarkChecked) {
            checkedChapter.setCheckedAt(checkedAt);
            chapterMapper.updateById(checkedChapter);
        }
        return CheckResponse.builder()
                .issueCount(issues.size())
                .issues(issues)
                .summary("mock 检查完成：重点提示连续性、人物状态、时间线、地点移动、设定冲突和 AI 痕迹风险。")
                .build();
    }

    private List<CheckIssueResponse> mockIssues(CheckRequest request) {
        return List.of(
                CheckIssueResponse.builder()
                        .type("continuity")
                        .severity("medium")
                        .description("请确认本章人物目标是否延续上一阶段，避免突然改变动机。")
                        .suggestion("在章节开头补一句承接上一章选择的心理或行动。")
                        .reference("连续性检查")
                        .build(),
                CheckIssueResponse.builder()
                        .type("ai_trace")
                        .severity("low")
                        .description("部分表达可能偏模板化，番茄平台发布前建议人工改写高频套话。")
                        .suggestion("减少泛化总结句，增加具体动作、细节和人物独特表达。")
                        .reference(request.getCheckType())
                        .build());
    }
}
