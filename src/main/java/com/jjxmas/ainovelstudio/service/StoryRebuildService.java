package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildRunResponse;

public interface StoryRebuildService {

    StoryRebuildResult rebuildFromChapter(Long projectId, Integer startChapterNo, Long modelConfigId);

    StoryRebuildResult rebuildFromEarliestDirty(Long projectId, Long modelConfigId);

    StoryRebuildRunResponse enqueueRebuild(Long projectId, Integer startChapterNo, Long modelConfigId);

    StoryRebuildRunResponse getRebuildRun(Long projectId, Long runId);

    StoryRebuildRunResponse getLatestRebuildRun(Long projectId);

    StoryRebuildResult processQueuedRebuild(Long runId, Runnable heartbeat);

    void markQueueFailure(Long runId, String errorMessage, boolean retry);
}
