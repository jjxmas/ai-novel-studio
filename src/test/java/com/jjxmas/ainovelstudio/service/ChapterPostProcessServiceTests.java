package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ChapterPostProcessRunMapper;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterPostProcessRun;
import com.jjxmas.ainovelstudio.service.impl.ChapterPostProcessServiceImpl;
import java.time.LocalDateTime;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class ChapterPostProcessServiceTests {

    @Test
    void enqueueCapturesCurrentContentVersion() {
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        Chapter chapter = new Chapter().setProjectId(1L).setLastContentVersionNo(4);
        chapter.setId(20L);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        ChapterPostProcessServiceImpl service = new ChapterPostProcessServiceImpl(
                chapterMapper,
                mock(ChapterPostProcessRunMapper.class),
                mock(ChapterMemoryService.class),
                mock(StoryDirtyMarkService.class),
                generationJobService,
                mock(CheckService.class));

        service.enqueueChapterAndMarkDirty(20L, 7L, "manual_edit", "调整正文");

        ArgumentCaptor<Map<String, Object>> input = ArgumentCaptor.forClass(Map.class);
        verify(generationJobService).enqueueJob(
                eq(1L),
                eq("chapter_post_process"),
                eq("chapter"),
                eq(20L),
                eq(7L),
                input.capture(),
                eq("20:4"),
                eq(0),
                any(LocalDateTime.class));
        assertThat(input.getValue().get("contentVersion")).isEqualTo(4);
        assertThat(input.getValue().get("dirtyReason")).isEqualTo("manual_edit");
    }

    @Test
    void queuedPostProcessSkipsObsoleteContentVersion() {
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService chapterMemoryService = mock(ChapterMemoryService.class);
        Chapter chapter = new Chapter().setProjectId(1L).setLastContentVersionNo(5);
        chapter.setId(20L);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        ChapterPostProcessServiceImpl service = new ChapterPostProcessServiceImpl(
                chapterMapper,
                mock(ChapterPostProcessRunMapper.class),
                chapterMemoryService,
                mock(StoryDirtyMarkService.class),
                mock(GenerationJobService.class),
                mock(CheckService.class));

        var result = service.refreshQueuedChapter(20L, 4, 7L, null, null, 10L);

        assertThat(result.getStatus()).isEqualTo("skipped");
        verify(chapterMemoryService, never()).refreshAfterChapterContent(any(), any());
    }

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
                mock(ChapterPostProcessRunMapper.class),
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
                mock(ChapterPostProcessRunMapper.class),
                chapterMemoryService,
                storyDirtyMarkService,
                generationJobService,
                checkService);

        var result = service.refreshChapter(20L, 7L);

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getErrorMessage()).isEqualTo("检查服务不可用");
        verify(chapterMemoryService).refreshAfterChapterContent(chapter, 7L);
    }

    @Test
    void queuedRetryContinuesAfterLastCompletedStep() {
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterPostProcessRunMapper runMapper = mock(ChapterPostProcessRunMapper.class);
        ChapterMemoryService chapterMemoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService storyDirtyMarkService = mock(StoryDirtyMarkService.class);
        CheckService checkService = mock(CheckService.class);
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(2)
                .setContent("正文")
                .setLastContentVersionNo(4);
        chapter.setId(20L);
        ChapterPostProcessRun run = new ChapterPostProcessRun()
                .setProjectId(1L)
                .setChapterId(20L)
                .setContentVersionNo(4)
                .setStatus("failed")
                .setCompletedStep(1)
                .setQualityIssueCount(0);
        run.setId(30L);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(runMapper.selectOne(any())).thenReturn(run);
        when(checkService.runCheck(any())).thenReturn(CheckResponse.builder()
                .issueCount(0)
                .summary("检查完成")
                .build());
        ChapterPostProcessServiceImpl service = new ChapterPostProcessServiceImpl(
                chapterMapper,
                runMapper,
                chapterMemoryService,
                storyDirtyMarkService,
                mock(GenerationJobService.class),
                checkService);

        var result = service.refreshQueuedChapter(20L, 4, 7L, "manual_edit", "调整正文", 10L);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(run.getCompletedStep()).isEqualTo(4);
        assertThat(run.getStatus()).isEqualTo("completed");
        verify(chapterMemoryService, never()).refreshFactProjection(any(), any());
        verify(chapterMemoryService).refreshNarrativeMemory(chapter, 7L);
        verify(storyDirtyMarkService).markDownstreamDirty(chapter, "manual_edit", "调整正文");
        verify(checkService).runCheck(any());
    }
}
