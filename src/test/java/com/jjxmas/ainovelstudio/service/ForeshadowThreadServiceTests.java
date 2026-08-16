package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadChangeMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadMapper;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThread;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThreadChange;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ForeshadowThreadServiceTests {

    @Test
    void historicalContextKeepsThreadResolvedLaterWithoutFutureProgress() {
        ForeshadowThreadMapper mapper = mock(ForeshadowThreadMapper.class);
        ForeshadowThread resolvedLater = new ForeshadowThread()
                .setProjectId(1L)
                .setThreadKey("copper-bell")
                .setThreadTitle("铜铃之谜")
                .setThreadType("foreshadow")
                .setStatus("resolved")
                .setSourceChapterNo(1)
                .setLastMentionedChapterNo(5)
                .setResolutionChapterNo(5)
                .setSetupText("第一章出现一枚无主铜铃")
                .setLatestProgress("第五章确认铜铃属于凶手")
                .setPayoffHint("凶手身份已经揭晓");
        resolvedLater.setId(10L);
        ForeshadowThread resolvedNow = new ForeshadowThread()
                .setProjectId(1L)
                .setThreadKey("closed")
                .setThreadTitle("已经结束")
                .setThreadType("foreshadow")
                .setStatus("resolved")
                .setSourceChapterNo(1)
                .setResolutionChapterNo(2);
        resolvedNow.setId(11L);
        when(mapper.selectList(any())).thenReturn(List.of(resolvedLater, resolvedNow));
        ForeshadowThreadService service = new ForeshadowThreadService(
                mapper,
                mock(ForeshadowThreadChangeMapper.class),
                mock(ContentVersionMapper.class));
        Chapter chapter = new Chapter().setProjectId(1L).setChapterNo(2);
        chapter.setId(20L);

        var threads = service.buildActiveThreads(chapter, "铜铃之谜", "", List.of(), "");

        assertThat(threads.getActiveForeshadowThreads()).singleElement()
                .satisfies(text -> assertThat(text)
                        .contains("第一章出现一枚无主铜铃")
                        .doesNotContain("第五章确认", "凶手身份已经揭晓", "已经结束"));
    }

    @Test
    void rebuildProjectionReplaysChangesInChapterOrder() {
        ForeshadowThreadMapper threadMapper = mock(ForeshadowThreadMapper.class);
        ForeshadowThreadChangeMapper changeMapper = mock(ForeshadowThreadChangeMapper.class);
        ForeshadowThreadChange setup = change(1L, 1, "mention", "铜铃出现");
        ForeshadowThreadChange payoff = change(2L, 5, "resolved", "确认铜铃属于凶手");
        when(changeMapper.selectList(any())).thenReturn(List.of(setup, payoff));
        ForeshadowThreadService service = new ForeshadowThreadService(
                threadMapper,
                changeMapper,
                mock(ContentVersionMapper.class));

        service.rebuildProjection(1L);

        ArgumentCaptor<ForeshadowThread> captor = ArgumentCaptor.forClass(ForeshadowThread.class);
        verify(threadMapper).insert(captor.capture());
        assertThat(captor.getValue())
                .satisfies(thread -> {
                    assertThat(thread.getSourceChapterNo()).isEqualTo(1);
                    assertThat(thread.getLastMentionedChapterNo()).isEqualTo(5);
                    assertThat(thread.getStatus()).isEqualTo("resolved");
                    assertThat(thread.getResolutionChapterNo()).isEqualTo(5);
                    assertThat(thread.getLatestProgress()).isEqualTo("确认铜铃属于凶手");
                });
    }

    private ForeshadowThreadChange change(Long id, int chapterNo, String changeType, String progress) {
        ForeshadowThreadChange change = new ForeshadowThreadChange()
                .setProjectId(1L)
                .setThreadKey("copper-bell")
                .setThreadTitle("铜铃之谜")
                .setThreadType("foreshadow")
                .setChangeKind("foreshadow")
                .setChangeType(changeType)
                .setPriority(70)
                .setChapterId((long) chapterNo)
                .setChapterNo(chapterNo)
                .setOriginChapterId(1L)
                .setOriginChapterNo(1)
                .setSetupText("第一章出现一枚无主铜铃")
                .setProgressText(progress);
        change.setId(id);
        return change;
    }
}
