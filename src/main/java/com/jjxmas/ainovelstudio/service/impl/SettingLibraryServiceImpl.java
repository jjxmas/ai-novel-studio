package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.service.SettingLibraryService;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 设定库服务实现，负责项目设定库的生成、维护、确认和版本记录。
 */
public class SettingLibraryServiceImpl extends ServiceImpl<SettingLibraryMapper, SettingLibrary> implements SettingLibraryService {

    private final ProjectMapper projectMapper;
    private final IdeaMapper ideaMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;

    /**
     * 注入设定库流程所需的 Mapper 和服务。
     */
    public SettingLibraryServiceImpl(
            ProjectMapper projectMapper,
            IdeaMapper ideaMapper,
            GenerationJobService generationJobService,
            VersionService versionService) {
        this.projectMapper = projectMapper;
        this.ideaMapper = ideaMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
    }

    /**
     * 基于选中创意生成或覆盖项目设定库。
     */
    @Override
    @Transactional
    public SettingLibraryResponse generateSettingLibrary(SettingLibraryGenerateRequest request) {
        Project project = requireProject(request.getProjectId());
        Idea selectedIdea = requireSelectedIdea(project, request.getIdeaId());
        String sourceIdeaSummary = request.getSourceIdeaSummary() == null || request.getSourceIdeaSummary().isBlank()
                ? selectedIdea.getSummary()
                : request.getSourceIdeaSummary();
        String summary = """
                【世界观基底】
                %s

                【核心人物】
                主角：目标明确但经验不足，需要在长期冲突中逐步成长。
                对手：代表既有秩序，持续制造压力。

                【地点与移动】
                初始地点负责新手引导，中段地点扩大资源与势力边界，后期地点承接主线决战。

                【规则边界】
                能力成长必须付出代价；重要设定首次出现后不得随意改写；伏笔需要在后续卷回收。
                """.formatted(sourceIdeaSummary);
        SettingLibrary setting = findByProjectId(project.getId());
        if (setting == null) {
            setting = new SettingLibrary().setProjectId(project.getId());
        }
        setting.setSummary(summary).setConfirmedAt(null);
        saveOrUpdate(setting);

        Map<String, Object> snapshot = Map.of("summary", setting.getSummary(), "confirmed", false);
        Long jobId = generationJobService.recordFinishedJob(
                project.getId(),
                "setting_generation",
                "setting_library",
                setting.getId(),
                request.getModelConfigId(),
                Map.of("ideaId", selectedIdea.getId(), "sourceIdeaSummary", sourceIdeaSummary),
                snapshot);
        versionService.recordVersion(project.getId(), "setting_library", setting.getId(), snapshot, "ai_generate", "mock 生成设定库", request.getModelConfigId(), jobId);
        return toResponse(setting);
    }

    /**
     * 查询指定项目的设定库详情。
     */
    @Override
    public SettingLibraryResponse getSettingLibrary(Long projectId) {
        requireProject(projectId);
        SettingLibrary setting = findByProjectId(projectId);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定库不存在");
        }
        return toResponse(setting);
    }

    /**
     * 按项目 ID 更新设定库正文并记录用户编辑版本。
     */
    @Override
    @Transactional
    public SettingLibraryResponse updateSettingLibrary(Long projectId, SettingLibraryUpdateRequest request) {
        SettingLibrary setting = requireSetting(projectId);
        setting.setSummary(request.getSummary()).setConfirmedAt(null);
        updateById(setting);
        versionService.recordVersion(
                projectId,
                "setting_library",
                setting.getId(),
                Map.of("summary", setting.getSummary(), "confirmed", false),
                "user_edit",
                request.getChangeNote() == null ? "用户直接修改设定库" : request.getChangeNote(),
                null,
                null);
        return toResponse(setting);
    }

    /**
     * 按设定库 ID 更新设定库正文。
     */
    @Override
    @Transactional
    public SettingLibraryResponse updateSettingLibraryById(Long settingLibraryId, SettingLibraryUpdateRequest request) {
        SettingLibrary setting = requireSettingById(settingLibraryId);
        return updateSettingLibrary(setting.getProjectId(), request);
    }

    /**
     * 根据用户指令重写设定库并记录 AI 重写版本。
     */
    @Override
    @Transactional
    public SettingLibraryResponse rewriteSettingLibrary(Long projectId, SettingLibraryRewriteRequest request) {
        SettingLibrary setting = requireSetting(projectId);
        setting.setSummary(setting.getSummary() + "\n\n【根据修改意见补充】\n" + request.getInstruction())
                .setConfirmedAt(null);
        updateById(setting);
        Map<String, Object> snapshot = Map.of("summary", setting.getSummary(), "confirmed", false);
        Long jobId = generationJobService.recordFinishedJob(
                projectId,
                "setting_rewrite",
                "setting_library",
                setting.getId(),
                request.getModelConfigId(),
                Map.of("instruction", request.getInstruction()),
                snapshot);
        versionService.recordVersion(projectId, "setting_library", setting.getId(), snapshot, "ai_rewrite", "根据用户修改意见重生成设定库", request.getModelConfigId(), jobId);
        return toResponse(setting);
    }

    /**
     * 确认项目设定库并推进项目状态。
     */
    @Override
    @Transactional
    public SettingLibraryResponse confirmSettingLibrary(Long projectId) {
        SettingLibrary setting = requireSetting(projectId);
        setting.setConfirmedAt(LocalDateTime.now());
        updateById(setting);
        Project project = requireProject(projectId);
        project.setStatus("setting_confirmed");
        projectMapper.updateById(project);
        versionService.recordVersion(
                projectId,
                "setting_library",
                setting.getId(),
                Map.of("summary", setting.getSummary(), "confirmed", true),
                "confirm",
                "确认设定库",
                null,
                null);
        return toResponse(setting);
    }

    /**
     * 按设定库 ID 确认设定库。
     */
    @Override
    @Transactional
    public SettingLibraryResponse confirmSettingLibraryById(Long settingLibraryId) {
        SettingLibrary setting = requireSettingById(settingLibraryId);
        return confirmSettingLibrary(setting.getProjectId());
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
     * 获取项目已选创意或请求指定创意。
     */
    private Idea requireSelectedIdea(Project project, Long ideaId) {
        Long selectedIdeaId = ideaId == null ? project.getSelectedIdeaId() : ideaId;
        if (selectedIdeaId == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先选择创意，再生成设定库");
        }
        Idea idea = ideaMapper.selectById(selectedIdeaId);
        if (idea == null || !project.getId().equals(idea.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创意不存在");
        }
        return idea;
    }

    /**
     * 获取项目设定库，不存在时抛出业务异常。
     */
    private SettingLibrary requireSetting(Long projectId) {
        requireProject(projectId);
        SettingLibrary setting = findByProjectId(projectId);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定库不存在");
        }
        return setting;
    }

    /**
     * 按设定库 ID 获取实体，不存在时抛出业务异常。
     */
    private SettingLibrary requireSettingById(Long settingLibraryId) {
        SettingLibrary setting = getById(settingLibraryId);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设定库不存在");
        }
        return setting;
    }

    /**
     * 查询指定项目的第一条设定库记录。
     */
    private SettingLibrary findByProjectId(Long projectId) {
        return getOne(new LambdaQueryWrapper<SettingLibrary>().eq(SettingLibrary::getProjectId, projectId).last("LIMIT 1"));
    }

    /**
     * 将设定库实体转换为接口响应对象。
     */
    private SettingLibraryResponse toResponse(SettingLibrary setting) {
        return SettingLibraryResponse.builder()
                .id(setting.getId())
                .projectId(setting.getProjectId())
                .summary(setting.getSummary())
                .charactersSummary("已写入设定库正文的【核心人物】段")
                .locationsSummary("已写入设定库正文的【地点与移动】段")
                .rulesSummary("已写入设定库正文的【规则边界】段")
                .confirmed(setting.getConfirmedAt() != null)
                .build();
    }
}
