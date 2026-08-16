package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.EntityRelation;
import com.jjxmas.ainovelstudio.pojo.entity.EntityStateRecord;
import com.jjxmas.ainovelstudio.pojo.entity.StoryCharacter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoryStateSnapshotServiceTests {

    @Test
    void snapshotExcludesFutureStateAndKeepsRelationEndingLater() {
        StoryCharacterMapper characterMapper = mock(StoryCharacterMapper.class);
        OrganizationMapper organizationMapper = mock(OrganizationMapper.class);
        StoryLocationMapper locationMapper = mock(StoryLocationMapper.class);
        StoryItemMapper itemMapper = mock(StoryItemMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        StoryEventMapper eventMapper = mock(StoryEventMapper.class);
        EntityRelationMapper relationMapper = mock(EntityRelationMapper.class);
        EntityStateRecordMapper stateMapper = mock(EntityStateRecordMapper.class);

        StoryCharacter character = new StoryCharacter()
                .setProjectId(1L)
                .setName("林川")
                .setStatus("active");
        character.setId(10L);
        when(characterMapper.selectList(any())).thenReturn(List.of(character));
        when(organizationMapper.selectList(any())).thenReturn(List.of());
        when(locationMapper.selectList(any())).thenReturn(List.of());
        when(itemMapper.selectList(any())).thenReturn(List.of());

        Chapter chapter1 = chapter(1L, 1);
        Chapter chapter2 = chapter(2L, 2);
        Chapter chapter3 = chapter(3L, 3);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter1, chapter2));
        when(chapterMapper.selectByIds(any())).thenReturn(List.of(chapter1, chapter3));

        StoryEvent relationStart = event(100L, 1L);
        StoryEvent relationEnd = event(200L, 3L);
        when(eventMapper.selectByIds(any())).thenReturn(List.of(relationStart, relationEnd));
        EntityRelation relation = new EntityRelation()
                .setProjectId(1L)
                .setSourceType("character")
                .setSourceId(10L)
                .setTargetType("character")
                .setTargetId(10L)
                .setRelationType("盟友")
                .setRelationStatus("ended")
                .setStartChapterNo(1)
                .setEndChapterNo(3)
                .setStartEventId(100L)
                .setEndEventId(200L);
        relation.setId(300L);
        EntityRelation futureRelationWithoutEvents = new EntityRelation()
                .setProjectId(1L)
                .setSourceType("character")
                .setSourceId(10L)
                .setTargetType("character")
                .setTargetId(10L)
                .setRelationType("未来敌对")
                .setRelationStatus("active")
                .setStartChapterNo(3);
        futureRelationWithoutEvents.setId(301L);
        when(relationMapper.selectList(any())).thenReturn(List.of(relation, futureRelationWithoutEvents));

        EntityStateRecord chapter1State = state(401L, 1L, "健康");
        EntityStateRecord chapter2State = state(402L, 2L, "受伤");
        EntityStateRecord futureState = state(403L, 3L, "死亡");
        when(stateMapper.selectList(any())).thenReturn(List.of(futureState, chapter2State, chapter1State));

        StoryStateSnapshotService service = new StoryStateSnapshotService(
                characterMapper,
                organizationMapper,
                locationMapper,
                itemMapper,
                chapterMapper,
                eventMapper,
                relationMapper,
                stateMapper);

        var snapshot = service.snapshotForChapter(chapter2, "林川", "", List.of(), "");

        assertThat(snapshot.getRelevantRelations()).singleElement()
                .satisfies(text -> assertThat(text).contains("盟友").doesNotContain("未来敌对"));
        assertThat(snapshot.getRelevantStateRecords()).singleElement()
                .satisfies(text -> assertThat(text)
                        .contains("受伤")
                        .doesNotContain("死亡", "健康"));
    }

    private Chapter chapter(Long id, int chapterNo) {
        Chapter chapter = new Chapter().setProjectId(1L).setChapterNo(chapterNo);
        chapter.setId(id);
        return chapter;
    }

    private StoryEvent event(Long id, Long chapterId) {
        StoryEvent event = new StoryEvent().setProjectId(1L).setChapterId(chapterId);
        event.setId(id);
        return event;
    }

    private EntityStateRecord state(Long id, Long chapterId, String value) {
        EntityStateRecord state = new EntityStateRecord()
                .setProjectId(1L)
                .setEntityType("character")
                .setEntityId(10L)
                .setStateType("生命状态")
                .setChapterId(chapterId)
                .setNewValue(Map.of("value", value));
        state.setId(id);
        return state;
    }
}
