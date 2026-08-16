package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StoryFactProjectionServiceTests {

    @Test
    void rewritingChapterReopensEarlierRelationAndDeletesRelationStartedHere() {
        StoryEventMapper eventMapper = mock(StoryEventMapper.class);
        EntityRelationMapper relationMapper = mock(EntityRelationMapper.class);
        ContentVersionMapper versionMapper = mock(ContentVersionMapper.class);
        EntityRelation endedHere = new EntityRelation()
                .setRelationStatus("ended")
                .setStartEventId(100L)
                .setEndChapterId(2L)
                .setEndChapterNo(2)
                .setEndContentVersionId(900L);
        endedHere.setId(30L);
        EntityRelation startedHere = new EntityRelation()
                .setRelationStatus("active")
                .setStartChapterId(2L)
                .setStartChapterNo(2);
        startedHere.setId(31L);
        ContentVersion version = new ContentVersion();
        version.setId(900L);

        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(relationMapper.selectList(any())).thenReturn(List.of(endedHere, startedHere));
        when(versionMapper.selectOne(any())).thenReturn(version);
        StoryFactProjectionService service = new StoryFactProjectionService(
                eventMapper,
                mock(EntityStateRecordMapper.class),
                relationMapper,
                mock(StoryCharacterMapper.class),
                mock(OrganizationMapper.class),
                mock(StoryLocationMapper.class),
                mock(StoryItemMapper.class),
                versionMapper);
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(2)
                .setLastContentVersionNo(3);
        chapter.setId(2L);

        service.projectChapterFacts(chapter, ChapterFactExtraction.builder().build());

        ArgumentCaptor<EntityRelation> captor = ArgumentCaptor.forClass(EntityRelation.class);
        verify(relationMapper).updateById(captor.capture());
        assertThat(captor.getValue())
                .satisfies(relation -> {
                    assertThat(relation.getRelationStatus()).isEqualTo("active");
                    assertThat(relation.getStartEventId()).isEqualTo(100L);
                    assertThat(relation.getEndEventId()).isNull();
                    assertThat(relation.getEndChapterId()).isNull();
                    assertThat(relation.getEndContentVersionId()).isNull();
                });
        verify(relationMapper).deleteById(31L);
        verify(relationMapper, never()).deleteById(30L);
    }
}
