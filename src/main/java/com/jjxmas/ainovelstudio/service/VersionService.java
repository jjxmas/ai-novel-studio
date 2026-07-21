package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.VersionResponse;
import java.util.List;
import java.util.Map;

/**
 * 版本服务，提供内容快照记录和查询能力。
 */
public interface VersionService {

    /**
     * 按版本 ID 查询版本快照。
     */
    VersionResponse getVersion(Long versionId);

    /**
     * 按项目、实体类型或实体 ID 查询版本列表。
     */
    List<VersionResponse> listVersions(Long projectId, String entityType, Long entityId);

    /**
     * 为指定实体记录新的内容版本快照。
     */
    void recordVersion(
            Long projectId,
            String entityType,
            Long entityId,
            Map<String, Object> snapshot,
            String changeSource,
            String changeNote,
            Long modelConfigId,
            Long jobId);
}
