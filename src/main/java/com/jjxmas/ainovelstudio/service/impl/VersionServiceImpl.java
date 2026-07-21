package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.pojo.dto.VersionResponse;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 版本服务。第二版只做快照写入和查询，不做回滚和对比。
 */
/**
 * 版本服务实现，负责内容版本快照写入和查询。
 */
@Service
public class VersionServiceImpl implements VersionService {

    private final ContentVersionMapper contentVersionMapper;

    /**
     * 注入内容版本 Mapper。
     */
    public VersionServiceImpl(ContentVersionMapper contentVersionMapper) {
        this.contentVersionMapper = contentVersionMapper;
    }

    /**
     * 查询指定版本快照，不存在时抛出业务异常。
     */
    @Override
    public VersionResponse getVersion(Long versionId) {
        ContentVersion version = contentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return toResponse(version);
    }

    /**
     * 按项目、实体类型或实体 ID 组合筛选版本快照。
     */
    @Override
    public List<VersionResponse> listVersions(Long projectId, String entityType, Long entityId) {
        LambdaQueryWrapper<ContentVersion> query = new LambdaQueryWrapper<ContentVersion>()
                .eq(projectId != null, ContentVersion::getProjectId, projectId)
                .eq(entityType != null && !entityType.isBlank(), ContentVersion::getEntityType, entityType)
                .eq(entityId != null, ContentVersion::getEntityId, entityId)
                .orderByDesc(ContentVersion::getCreatedAt);
        return contentVersionMapper.selectList(query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 为实体追加一条递增版本号的内容快照。
     */
    @Override
    @Transactional
    public void recordVersion(
            Long projectId,
            String entityType,
            Long entityId,
            Map<String, Object> snapshot,
            String changeSource,
            String changeNote,
            Long modelConfigId,
            Long jobId) {
        ContentVersion latestVersion = contentVersionMapper.selectOne(new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getEntityType, entityType)
                .eq(ContentVersion::getEntityId, entityId)
                .orderByDesc(ContentVersion::getVersionNo)
                .last("LIMIT 1"));
        int nextVersionNo = latestVersion == null ? 1 : latestVersion.getVersionNo() + 1;

        ContentVersion version = new ContentVersion()
                .setProjectId(projectId)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setVersionNo(nextVersionNo)
                .setSnapshot(JsonUtils.toJson(snapshot))
                .setChangeSource(changeSource)
                .setOperationType(toOperationType(changeSource))
                .setChangeNote(changeNote)
                .setRevisionInstruction(changeNote)
                .setModelConfigId(modelConfigId)
                .setJobId(jobId);
        contentVersionMapper.insert(version);
    }

    /**
     * 将内容版本实体转换为版本响应对象。
     */
    private VersionResponse toResponse(ContentVersion version) {
        return VersionResponse.builder()
                .id(version.getId())
                .entityType(version.getEntityType())
                .entityId(version.getEntityId())
                .versionNo(version.getVersionNo())
                .changeSource(version.getChangeSource())
                .changeNote(version.getChangeNote())
                .snapshot(version.getSnapshot())
                .build();
    }

    /**
     * 根据变更来源推断版本操作类型。
     */
    private String toOperationType(String changeSource) {
        if (changeSource == null) {
            return "generate";
        }
        if (changeSource.contains("edit")) {
            return "edit";
        }
        if (changeSource.contains("rewrite")) {
            return "rewrite";
        }
        if ("confirm".equals(changeSource)) {
            return "confirm";
        }
        if ("export".equals(changeSource)) {
            return "export";
        }
        return "generate";
    }
}
