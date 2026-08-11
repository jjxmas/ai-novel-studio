package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.VersionConverter;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.pojo.dto.VersionResponse;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionServiceImpl implements VersionService {

    private final ContentVersionMapper contentVersionMapper;
    private final VersionConverter versionConverter;

    public VersionServiceImpl(
            ContentVersionMapper contentVersionMapper,
            VersionConverter versionConverter) {
        this.contentVersionMapper = contentVersionMapper;
        this.versionConverter = versionConverter;
    }

    @Override
    public VersionResponse getVersion(Long versionId) {
        ContentVersion version = contentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "版本不存在");
        }
        return versionConverter.toResponse(version);
    }

    @Override
    public List<VersionResponse> listVersions(Long projectId, String entityType, Long entityId) {
        LambdaQueryWrapper<ContentVersion> query = new LambdaQueryWrapper<ContentVersion>()
                .eq(projectId != null, ContentVersion::getProjectId, projectId)
                .eq(entityType != null && !entityType.isBlank(), ContentVersion::getEntityType, entityType)
                .eq(entityId != null, ContentVersion::getEntityId, entityId)
                .orderByDesc(ContentVersion::getCreatedAt);
        return versionConverter.toResponseList(contentVersionMapper.selectList(query));
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
