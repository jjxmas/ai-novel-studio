package com.jjxmas.ainovelstudio.module.version.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.module.version.dto.VersionResponse;
import com.jjxmas.ainovelstudio.module.version.entity.ContentVersion;
import com.jjxmas.ainovelstudio.module.version.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.module.version.service.VersionService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 版本服务。第二版只做快照写入和查询，不做回滚和对比。
 */
@Service
public class VersionServiceImpl implements VersionService {

    private final ContentVersionMapper contentVersionMapper;

    public VersionServiceImpl(ContentVersionMapper contentVersionMapper) {
        this.contentVersionMapper = contentVersionMapper;
    }

    @Override
    public VersionResponse getVersion(Long versionId) {
        ContentVersion version = contentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return toResponse(version);
    }

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
