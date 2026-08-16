package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterFactExtractionRunMapper;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ChapterFactExtractionRun;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class ChapterFactExtractionServiceTests {

    @Test
    void reusesExtractionForSameContentVersion() {
        AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
        ChapterFactExtractionRunMapper runMapper = mock(ChapterFactExtractionRunMapper.class);
        ContentVersionMapper contentVersionMapper = mock(ContentVersionMapper.class);
        ContentVersion contentVersion = new ContentVersion().setVersionNo(4);
        contentVersion.setId(99L);
        ChapterFactExtraction extraction = ChapterFactExtraction.builder()
                .events(List.of())
                .stateChanges(List.of())
                .relationChanges(List.of())
                .foreshadowChanges(List.of())
                .unresolvedThreads(List.of())
                .issues(List.of())
                .build();
        ChapterFactExtractionRun run = new ChapterFactExtractionRun()
                .setChapterId(20L)
                .setSourceContentVersionId(99L)
                .setNormalizedOutputJson(JsonUtils.toJson(extraction));
        when(contentVersionMapper.selectOne(any())).thenReturn(contentVersion);
        when(runMapper.selectOne(any())).thenReturn(run);
        ChapterFactExtractionService service = new ChapterFactExtractionService(
                aiOrchestratorService,
                mock(GenerationJobService.class),
                mock(VersionService.class),
                runMapper,
                contentVersionMapper,
                mock(TransactionTemplate.class));
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(2)
                .setLastContentVersionNo(4)
                .setContent("正文");
        chapter.setId(20L);

        ChapterFactExtraction result = service.extractAndStore(chapter, 7L);

        assertThat(result.getIssues()).isEmpty();
        verifyNoInteractions(aiOrchestratorService);
    }
}
