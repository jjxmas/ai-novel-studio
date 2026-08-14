package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.service.impl.ChapterPostProcessServiceImpl;
import org.junit.jupiter.api.Test;

class ChapterPostProcessServiceTests {

    @Test
    void runsContinuityCheckAfterChapterPostProcess() {
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService chapterMemoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService storyDirtyMarkService = mock(StoryDirtyMarkService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        CheckService checkService = mock(CheckService.class);
        Chapter chapter = new Chapter().setProjectId(1L).setChapterNo(2).setContent("正文");
        chapter.setId(20L);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(checkService.runCheck(any())).thenReturn(CheckResponse.builder()
                .issueCount(1)
                .summary("检查完成")
                .build());

        ChapterPostProcessServiceImpl service = new ChapterPostProcessServiceImpl(
                chapterMapper,
                chapterMemoryService,
                storyDirtyMarkService,
                generationJobService,
                checkService);

        var result = service.refreshChapter(20L, 7L);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getIssueCount()).isEqualTo(1);
        verify(chapterMemoryService).refreshAfterChapterContent(chapter, 7L);
        verify(checkService).runCheck(any());
    }

    @Test
    void reportsQualityFailureWithoutFailingGeneratedContent() {
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService chapterMemoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService storyDirtyMarkService = mock(StoryDirtyMarkService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        CheckService checkService = mock(CheckService.class);
        Chapter chapter = new Chapter().setProjectId(1L).setChapterNo(2).setContent("正文");
        chapter.setId(20L);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(checkService.runCheck(any())).thenThrow(new IllegalStateException("检查服务不可用"));

        ChapterPostProcessServiceImpl service = new ChapterPostProcessServiceImpl(
                chapterMapper,
                chapterMemoryService,
                storyDirtyMarkService,
                generationJobService,
                checkService);

        var result = service.refreshChapter(20L, 7L);

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getErrorMessage()).isEqualTo("检查服务不可用");
        verify(chapterMemoryService).refreshAfterChapterContent(chapter, 7L);
    }
}
