package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.StoryDirtyMark;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.service.StoryDirtyMarkService;
import com.jjxmas.ainovelstudio.service.StoryRebuildService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryRebuildServiceImpl implements StoryRebuildService {

    private static final String NOT_FOUND_MESSAGE = "\u4f5c\u54c1\u4e0d\u5b58\u5728";
    private static final String NO_ACTIVE_DIRTY_MARK_MESSAGE =
            "\u6ca1\u6709\u53ef\u56de\u7b97\u7684 active dirty mark";
    private static final String NO_REBUILDABLE_CHAPTER_MESSAGE =
            "\u6ca1\u6709\u53ef\u91cd\u8dd1\u540e\u5904\u7406\u7684\u7ae0\u8282\u6b63\u6587";
    private static final String REBUILD_COMPLETED_MESSAGE =
            "\u5df2\u6309\u7ae0\u8282\u987a\u5e8f\u91cd\u8dd1\u4e8b\u5b9e\u62bd\u53d6\u3001\u4e8b\u5b9e\u6295\u5f71\u3001\u7ebf\u7a0b\u6295\u5f71\u548c\u6458\u8981\u8bb0\u5fc6\u94fe";

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterMemoryService chapterMemoryService;
    private final StoryDirtyMarkService storyDirtyMarkService;

    public StoryRebuildServiceImpl(
            ProjectMapper projectMapper,
            ChapterMapper chapterMapper,
            ChapterMemoryService chapterMemoryService,
            StoryDirtyMarkService storyDirtyMarkService) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.chapterMemoryService = chapterMemoryService;
        this.storyDirtyMarkService = storyDirtyMarkService;
    }

    @Override
    @Transactional
    public StoryRebuildResult rebuildFromChapter(Long projectId, Integer startChapterNo, Long modelConfigId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE);
        }

        Integer actualStartChapterNo = startChapterNo == null
                ? storyDirtyMarkService.earliestActiveDirtyChapterNo(projectId)
                : startChapterNo;
        if (actualStartChapterNo == null) {
            return StoryRebuildResult.builder()
                    .projectId(projectId)
                    .requestedStartChapterNo(startChapterNo)
                    .actualStartChapterNo(null)
                    .endChapterNo(null)
                    .processedChapterCount(0)
                    .skippedChapterCount(0)
                    .activeDirtyMarkCountBefore(0)
                    .resolvedDirtyMarkCount(0)
                    .activeDirtyMarkCountAfter(0)
                    .earliestDirtyChapterNoAfter(null)
                    .processedChapterNos(List.of())
                    .skippedChapterNos(List.of())
                    .status("noop")
                    .note(NO_ACTIVE_DIRTY_MARK_MESSAGE)
                    .build();
        }

        List<StoryDirtyMark> activeMarksBefore = storyDirtyMarkService.listActiveMarks(projectId);
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .ge(Chapter::getChapterNo, actualStartChapterNo)
                .orderByAsc(Chapter::getChapterNo));

        List<Integer> processedChapterNos = new ArrayList<>();
        List<Integer> skippedChapterNos = new ArrayList<>();
        for (Chapter chapter : chapters) {
            if (chapter.getContent() == null || chapter.getContent().isBlank()) {
                if (chapter.getChapterNo() != null) {
                    skippedChapterNos.add(chapter.getChapterNo());
                }
                continue;
            }
            chapterMemoryService.refreshAfterChapterContent(chapter, modelConfigId);
            if (chapter.getChapterNo() != null) {
                processedChapterNos.add(chapter.getChapterNo());
            }
        }

        int resolvedDirtyMarkCount = processedChapterNos.isEmpty()
                ? 0
                : storyDirtyMarkService.resolveActiveMarksFromChapter(projectId, actualStartChapterNo);
        List<StoryDirtyMark> remainingActiveMarks = storyDirtyMarkService.listActiveMarks(projectId);

        Integer endChapterNo = processedChapterNos.isEmpty()
                ? null
                : processedChapterNos.get(processedChapterNos.size() - 1);

        return StoryRebuildResult.builder()
                .projectId(projectId)
                .requestedStartChapterNo(startChapterNo)
                .actualStartChapterNo(actualStartChapterNo)
                .endChapterNo(endChapterNo)
                .processedChapterCount(processedChapterNos.size())
                .skippedChapterCount(skippedChapterNos.size())
                .activeDirtyMarkCountBefore(activeMarksBefore.size())
                .resolvedDirtyMarkCount(resolvedDirtyMarkCount)
                .activeDirtyMarkCountAfter(remainingActiveMarks.size())
                .earliestDirtyChapterNoAfter(storyDirtyMarkService.earliestActiveDirtyChapterNo(projectId))
                .processedChapterNos(processedChapterNos)
                .skippedChapterNos(skippedChapterNos)
                .status(processedChapterNos.isEmpty() ? "noop" : "completed")
                .note(processedChapterNos.isEmpty() ? NO_REBUILDABLE_CHAPTER_MESSAGE : REBUILD_COMPLETED_MESSAGE)
                .build();
    }

    @Override
    @Transactional
    public StoryRebuildResult rebuildFromEarliestDirty(Long projectId, Long modelConfigId) {
        return rebuildFromChapter(projectId, null, modelConfigId);
    }
}
