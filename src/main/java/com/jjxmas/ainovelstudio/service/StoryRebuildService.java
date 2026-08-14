package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult;

public interface StoryRebuildService {

    StoryRebuildResult rebuildFromChapter(Long projectId, Integer startChapterNo, Long modelConfigId);

    StoryRebuildResult rebuildFromEarliestDirty(Long projectId, Long modelConfigId);
}
