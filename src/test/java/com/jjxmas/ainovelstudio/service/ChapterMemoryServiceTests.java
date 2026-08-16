package com.jjxmas.ainovelstudio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.converter.ChapterMemoryConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.StoryMemoryMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterSummary;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.pojo.entity.StoryMemory;
import com.jjxmas.ainovelstudio.service.impl.ChapterMemoryServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class ChapterMemoryServiceTests {

    @Test
    void reusesSummaryForSameContentVersion() {
        ChapterSummaryMapper chapterSummaryMapper = mock(ChapterSummaryMapper.class);
        ContentVersionMapper contentVersionMapper = mock(ContentVersionMapper.class);
        StoryMemoryMapper storyMemoryMapper = mock(StoryMemoryMapper.class);
        AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ContentVersion contentVersion = new ContentVersion().setVersionNo(4);
        contentVersion.setId(99L);
        ChapterSummary summary = new ChapterSummary()
                .setProjectId(1L)
                .setChapterId(20L)
                .setChapterNo(2)
                .setSummary("已有摘要")
                .setSourceContentVersionId(99L);
        summary.setId(30L);
        StoryMemory currentWindow = new StoryMemory()
                .setProjectId(1L)
                .setMemoryType("recent_window")
                .setStartChapterNo(2)
                .setEndChapterNo(2)
                .setContent("第 2 章：已有摘要")
                .setSourceChapterSummaryIds("[30]")
                .setCurrent(true);
        when(contentVersionMapper.selectOne(any())).thenReturn(contentVersion);
        when(chapterSummaryMapper.selectOne(any())).thenReturn(summary);
        when(chapterSummaryMapper.selectList(any())).thenReturn(List.of(summary));
        when(storyMemoryMapper.selectList(any())).thenReturn(List.of(currentWindow));
        ChapterMemoryServiceImpl service = new ChapterMemoryServiceImpl(
                chapterSummaryMapper,
                contentVersionMapper,
                storyMemoryMapper,
                mock(ProjectMapper.class),
                aiOrchestratorService,
                mock(GenerationJobService.class),
                mock(VersionService.class),
                mock(ChapterMemoryConverter.class),
                mock(ChapterFactExtractionService.class),
                mock(ForeshadowThreadService.class),
                mock(StoryFactProjectionService.class),
                transactionTemplate);
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(2)
                .setLastContentVersionNo(4)
                .setContent("正文");
        chapter.setId(20L);

        service.refreshNarrativeMemory(chapter, 7L);

        verifyNoInteractions(aiOrchestratorService);
        verifyNoInteractions(transactionTemplate);
    }
}
