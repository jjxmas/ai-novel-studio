package com.jjxmas.ainovelstudio.module.version.service;

import com.jjxmas.ainovelstudio.module.version.dto.VersionResponse;
import java.util.List;
import java.util.Map;

public interface VersionService {

    VersionResponse getVersion(Long versionId);

    List<VersionResponse> listVersions(Long projectId, String entityType, Long entityId);

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

