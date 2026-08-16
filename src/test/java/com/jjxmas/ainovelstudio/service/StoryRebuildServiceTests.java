package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.StoryRebuildRunMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.StoryDirtyMark;
import com.jjxmas.ainovelstudio.pojo.entity.StoryRebuildRun;
import com.jjxmas.ainovelstudio.service.impl.StoryRebuildServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StoryRebuildServiceTests {

    @Test
    void enqueueCreatesDurableRunWithoutExecutingRebuild() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        StoryRebuildRunMapper runMapper = mock(StoryRebuildRunMapper.class);
        GenerationJobService jobService = mock(GenerationJobService.class);
        ChapterMemoryService memoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService dirtyMarkService = mock(StoryDirtyMarkService.class);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(dirtyMarkService.listActiveMarks(1L)).thenReturn(List.of());
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<StoryRebuildRun>getArgument(0).setId(50L);
            return 1;
        }).when(runMapper).insert(any(StoryRebuildRun.class));
        when(jobService.enqueueJob(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("story_rebuild"),
                org.mockito.ArgumentMatchers.eq("story_rebuild_run"),
                org.mockito.ArgumentMatchers.eq(50L),
                org.mockito.ArgumentMatchers.eq(9L),
                any(),
                org.mockito.ArgumentMatchers.eq("story-rebuild-run:50"),
                org.mockito.ArgumentMatchers.eq(5),
                any())).thenReturn(60L);
        StoryRebuildService service = new StoryRebuildServiceImpl(
                projectMapper, mock(ChapterMapper.class), memoryService, dirtyMarkService, runMapper, jobService);

        var response = service.enqueueRebuild(1L, 2, 9L);

        assertThat(response.getRunId()).isEqualTo(50L);
        assertThat(response.getGenerationJobId()).isEqualTo(60L);
        assertThat(response.getStatus()).isEqualTo("pending");
        verify(memoryService, never()).refreshFactProjection(any(), any());
    }

    @Test
    void queuedRebuildPersistsChapterCheckpointsAndHeartbeats() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService memoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService dirtyMarkService = mock(StoryDirtyMarkService.class);
        StoryRebuildRunMapper runMapper = mock(StoryRebuildRunMapper.class);
        StoryRebuildRun run = new StoryRebuildRun()
                .setProjectId(1L)
                .setModelConfigId(9L)
                .setRequestedStartChapterNo(2)
                .setActualStartChapterNo(2)
                .setPhase("fact_projection")
                .setNextFactChapterNo(2)
                .setMemoryResetDone(false)
                .setProcessedChapterNosJson("[]")
                .setSkippedChapterNosJson("[]")
                .setDirtyMarkIdsJson("[10]")
                .setActiveDirtyMarkCountBefore(1)
                .setStatus("pending");
        run.setId(50L);
        when(runMapper.selectById(50L)).thenReturn(run);
        Chapter chapter1 = chapter(1L, 1, "第一章正文");
        Chapter chapter2 = chapter(2L, 2, "第二章正文");
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter1, chapter2));
        when(dirtyMarkService.listActiveMarks(1L)).thenReturn(List.of());
        when(dirtyMarkService.countActiveMarksByIds(List.of(10L))).thenReturn(0);
        Runnable heartbeat = mock(Runnable.class);
        StoryRebuildService service = new StoryRebuildServiceImpl(
                projectMapper,
                chapterMapper,
                memoryService,
                dirtyMarkService,
                runMapper,
                mock(GenerationJobService.class));

        var result = service.processQueuedRebuild(50L, heartbeat);

        InOrder order = inOrder(memoryService);
        order.verify(memoryService).refreshFactProjection(chapter2, 9L);
        order.verify(memoryService).resetNarrativeMemory(1L);
        order.verify(memoryService).refreshNarrativeMemory(chapter1, 9L);
        order.verify(memoryService).refreshNarrativeMemory(chapter2, 9L);
        verify(heartbeat, times(3)).run();
        verify(dirtyMarkService).resolveActiveMarksByIds(List.of(10L));
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(run.getStatus()).isEqualTo("succeeded");
        assertThat(run.getPhase()).isEqualTo("completed");
        assertThat(run.getNextFactChapterNo()).isEqualTo(3);
        assertThat(run.getNextMemoryChapterNo()).isEqualTo(3);
    }

    @Test
    void rebuildsFactsFromDirtyChapterThenRebuildsMemoryFromBeginning() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService memoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService dirtyMarkService = mock(StoryDirtyMarkService.class);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Chapter chapter1 = chapter(1L, 1, "第一章正文");
        Chapter chapter2 = chapter(2L, 2, "第二章正文");
        Chapter chapter3 = chapter(3L, 3, "");
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter1, chapter2, chapter3));
        StoryDirtyMark capturedMark = new StoryDirtyMark().setDirtyFromChapterNo(2);
        capturedMark.setId(10L);
        when(dirtyMarkService.listActiveMarks(1L))
                .thenReturn(List.of(capturedMark))
                .thenReturn(List.of());
        when(dirtyMarkService.countActiveMarksByIds(List.of(10L))).thenReturn(0);
        StoryRebuildService service = new StoryRebuildServiceImpl(
                projectMapper,
                chapterMapper,
                memoryService,
                dirtyMarkService,
                mock(StoryRebuildRunMapper.class),
                mock(GenerationJobService.class));

        var result = service.rebuildFromChapter(1L, 2, 9L);

        InOrder order = inOrder(memoryService);
        order.verify(memoryService).refreshFactProjection(chapter2, 9L);
        order.verify(memoryService).clearFactProjection(chapter3);
        order.verify(memoryService).resetNarrativeMemory(1L);
        order.verify(memoryService).refreshNarrativeMemory(chapter1, 9L);
        order.verify(memoryService).refreshNarrativeMemory(chapter2, 9L);
        verify(memoryService, never()).refreshFactProjection(chapter1, 9L);
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getProcessedChapterNos()).containsExactly(2);
        assertThat(result.getSkippedChapterNos()).containsExactly(3);
        assertThat(result.getResolvedDirtyMarkCount()).isEqualTo(1);
        assertThat(result.getEndChapterNo()).isEqualTo(3);
    }

    @Test
    void failedReplayDoesNotResolveDirtyMarks() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterMemoryService memoryService = mock(ChapterMemoryService.class);
        StoryDirtyMarkService dirtyMarkService = mock(StoryDirtyMarkService.class);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        Chapter chapter = chapter(2L, 2, "第二章正文");
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        org.mockito.Mockito.doThrow(new IllegalStateException("projection failed"))
                .when(memoryService).refreshFactProjection(chapter, 9L);
        StoryRebuildService service = new StoryRebuildServiceImpl(
                projectMapper,
                chapterMapper,
                memoryService,
                dirtyMarkService,
                mock(StoryRebuildRunMapper.class),
                mock(GenerationJobService.class));

        assertThatThrownBy(() -> service.rebuildFromChapter(1L, 2, 9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("projection failed");
        verify(dirtyMarkService, never()).resolveActiveMarksByIds(any());
        verify(memoryService, never()).resetNarrativeMemory(any());
    }

    private Chapter chapter(Long id, int chapterNo, String content) {
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(chapterNo)
                .setContent(content)
                .setLastContentVersionNo(1);
        chapter.setId(id);
        return chapter;
    }
}
