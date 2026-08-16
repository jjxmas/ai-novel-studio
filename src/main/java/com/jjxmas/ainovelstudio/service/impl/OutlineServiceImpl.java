package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.converter.OutlineConverter;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.VolumeMapper;
import com.jjxmas.ainovelstudio.service.OutlineService;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 大纲服务实现，负责全局大纲、卷、剧情弧和章节大纲的生成与维护。
 */
public class OutlineServiceImpl extends ServiceImpl<OutlineMapper, Outline> implements OutlineService {

    private final ProjectMapper projectMapper;
    private final VolumeMapper volumeMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final OutlineConverter outlineConverter;
    private final CacheManager cacheManager;

    /**
     * 注入大纲流程所需的 Mapper、任务服务和版本服务。
     */
    public OutlineServiceImpl(
            ProjectMapper projectMapper,
            VolumeMapper volumeMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            OutlineConverter outlineConverter,
            CacheManager cacheManager) {
        this.projectMapper = projectMapper;
        this.volumeMapper = volumeMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.outlineConverter = outlineConverter;
        this.cacheManager = cacheManager;
    }

    /**
     * 查询指定项目的全局大纲。
     */
    @Override
    @Cacheable(value = "globalOutlines", key = "#projectId")
    public OutlineResponse getGlobalOutline(Long projectId) {
        requireProject(projectId);
        Outline outline = findByProjectId(projectId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return toResponse(outline);
    }

    /**
     * 按项目 ID 更新全局大纲并清除确认状态。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public void updateGlobalOutline(Long projectId, OutlineUpdateRequest request) {
        Outline outline = requireOutline(projectId);
        outline.setTitle(request.getTitle()).setContent(request.getContent()).setConfirmedAt(null);
        updateById(outline);
        evictOutlineCaches(projectId);
        versionService.recordVersion(
                projectId,
                "global_outline",
                outline.getId(),
                outlineSnapshot(outline),
                "user_edit",
                request.getChangeNote() == null ? "用户直接修改全局大纲" : request.getChangeNote(),
                null,
                null);
    }

    /**
     * 按大纲 ID 更新全局大纲。
     */
    @Override
    @Transactional
    public void updateGlobalOutlineById(Long outlineId, OutlineUpdateRequest request) {
        Outline outline = requireOutlineById(outlineId);
        updateGlobalOutline(outline.getProjectId(), request);
        evictOutlineCaches(outline.getProjectId());
    }

    /**
     * 根据用户指令重写全局大纲并记录版本。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public OutlineResponse rewriteGlobalOutline(Long projectId, OutlineRewriteRequest request) {
        Outline outline = requireOutline(projectId);
        outline.setContent(outline.getContent() + "\n\n【根据修改意见调整】\n" + request.getInstruction())
                .setConfirmedAt(null);
        updateById(outline);
        Map<String, Object> snapshot = outlineSnapshot(outline);
        Long jobId = generationJobService.recordFinishedJob(
                projectId,
                "global_outline_rewrite",
                "global_outline",
                outline.getId(),
                request.getModelConfigId(),
                Map.of("instruction", request.getInstruction()),
                snapshot);
        versionService.recordVersion(projectId, "global_outline", outline.getId(), snapshot, "ai_rewrite", "根据用户修改意见重生成全局大纲", request.getModelConfigId(), jobId);
        return toResponse(outline);
    }

    /**
     * 确认全局大纲并推进项目状态。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public void confirmGlobalOutline(Long projectId) {
        Outline outline = requireOutline(projectId);
        outline.setConfirmedAt(LocalDateTime.now());
        updateById(outline);
        evictOutlineCaches(projectId);
        Project project = requireProject(projectId);
        project.setWorkflowStage("chapter");
        projectMapper.updateById(project);
        versionService.recordVersion(
                projectId,
                "global_outline",
                outline.getId(),
                outlineSnapshot(outline),
                "confirm",
                "确认全局大纲",
                null,
                null);
    }

    /**
     * 按大纲 ID 确认全局大纲。
     */
    @Override
    @Transactional
    public void confirmGlobalOutlineById(Long outlineId) {
        Outline outline = requireOutlineById(outlineId);
        confirmGlobalOutline(outline.getProjectId());
        evictOutlineCaches(outline.getProjectId());
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
     * 获取项目全局大纲，不存在时抛出业务异常。
     */
    private Outline requireOutline(Long projectId) {
        requireProject(projectId);
        Outline outline = findByProjectId(projectId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return outline;
    }

    /**
     * 按大纲 ID 获取全局大纲，不存在时抛出业务异常。
     */
    private Outline requireOutlineById(Long outlineId) {
        Outline outline = getById(outlineId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return outline;
    }

    /**
     * 查询指定项目的第一条全局大纲。
     */
    private Outline findByProjectId(Long projectId) {
        return getOne(new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, projectId).last("LIMIT 1"));
    }

    private void evictOutlineCaches(Long projectId) {
        for (String cacheName : List.of("globalOutlines", "chapterContextOutlines")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(projectId);
            }
        }
    }

    /**
     * 将大纲实体和卷信息转换为响应对象。
     */
    private OutlineResponse toResponse(Outline outline) {
        OutlineResponse response = outlineConverter.toResponse(outline);
        response.setVolumes(outlineConverter.toVolumeResponseList(volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, outline.getProjectId())
                .orderByAsc(Volume::getVolumeNo))));
        return response;
    }

    /**
     * 构造全局大纲版本快照内容。
     */
    private Map<String, Object> outlineSnapshot(Outline outline) {
        return Map.of(
                "title", outline.getTitle(),
                "content", outline.getContent(),
                "confirmed", outline.getConfirmedAt() != null);
    }
}
