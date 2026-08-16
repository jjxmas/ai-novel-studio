package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ChapterSummaryMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.mapper.StoryArcMapper;
import com.jjxmas.ainovelstudio.mapper.VolumeMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterOutlineContinueRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.pojo.entity.StoryArc;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ChapterOutlineContinuationServiceTests {

    @Test
    void appendsFiftyContinuousChaptersInFiveModelCallsWithoutDeletingExistingChapters() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        SettingLibraryMapper settingLibraryMapper = mock(SettingLibraryMapper.class);
        OutlineMapper outlineMapper = mock(OutlineMapper.class);
        VolumeMapper volumeMapper = mock(VolumeMapper.class);
        StoryArcMapper storyArcMapper = mock(StoryArcMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterSummaryMapper chapterSummaryMapper = mock(ChapterSummaryMapper.class);
        ForeshadowThreadMapper foreshadowThreadMapper = mock(ForeshadowThreadMapper.class);
        AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        VersionService versionService = mock(VersionService.class);
        ChapterConverter chapterConverter = mock(ChapterConverter.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        Project project = new Project().setTitle("测试作品");
        project.setId(1L);
        Outline outline = new Outline()
                .setProjectId(1L)
                .setTitle("全局大纲")
                .setContent("全局故事规划")
                .setConfirmedAt(LocalDateTime.now());
        outline.setId(2L);
        SettingLibrary setting = new SettingLibrary()
                .setProjectId(1L)
                .setSummary("已确认设定")
                .setConfirmedAt(LocalDateTime.now());
        Volume volume = new Volume().setProjectId(1L).setVolumeNo(1).setTitle("第一卷");
        volume.setId(10L);
        StoryArc arc = new StoryArc().setProjectId(1L).setVolumeId(10L).setArcNo(1).setTitle("第一单元");
        arc.setId(20L);
        Chapter existingChapter = new Chapter().setProjectId(1L).setChapterNo(8).setTitle("第8章").setOutline("已有大纲");
        existingChapter.setId(8L);

        when(projectMapper.selectById(1L)).thenReturn(project);
        when(projectMapper.selectOne(any())).thenReturn(project);
        when(outlineMapper.selectOne(any())).thenReturn(outline);
        when(settingLibraryMapper.selectOne(any())).thenReturn(setting);
        when(volumeMapper.selectList(any())).thenReturn(List.of(volume));
        when(storyArcMapper.selectList(any())).thenReturn(List.of(arc));
        when(chapterMapper.selectOne(any())).thenReturn(existingChapter);
        when(chapterMapper.selectList(any())).thenReturn(List.of(existingChapter));
        when(chapterSummaryMapper.selectList(any())).thenReturn(List.of());
        when(foreshadowThreadMapper.selectList(any())).thenReturn(List.of());
        when(generationJobService.recordFinishedJob(
                anyLong(), any(), any(), anyLong(), anyLong(), any(), any())).thenReturn(100L);
        when(aiOrchestratorService.continueChapterOutline(eq(7L), any())).thenAnswer(invocation -> {
            Map<String, Object> context = invocation.getArgument(1);
            int start = (Integer) context.get("startChapterNo");
            int count = (Integer) context.get("count");
            List<Map<String, Object>> chapters = IntStream.range(start, start + count)
                    .mapToObj(chapterNo -> Map.<String, Object>of(
                            "chapterNo", chapterNo,
                            "volumeNo", 1,
                            "arcNo", 1,
                            "title", "第" + chapterNo + "章",
                            "outline", "续写章节大纲 " + chapterNo,
                            "scenePlan", List.of("目标", "冲突", "钩子")))
                    .toList();
            return AiGenerateResult.builder()
                    .success(true)
                    .content(JsonUtils.toJson(Map.of(
                            "newVolumes", List.of(),
                            "newArcs", List.of(),
                            "chapters", chapters)))
                    .build();
        });
        AtomicLong chapterId = new AtomicLong(1000);
        when(chapterMapper.insert(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(chapterId.incrementAndGet());
            return 1;
        });
        when(chapterConverter.toResponse(any())).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            return ChapterResponse.builder()
                    .id(chapter.getId())
                    .projectId(chapter.getProjectId())
                    .chapterNo(chapter.getChapterNo())
                    .title(chapter.getTitle())
                    .outline(chapter.getOutline())
                    .status(chapter.getStatus())
                    .build();
        });
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<List<ChapterResponse>>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));

        ChapterOutlineContinuationService service = new ChapterOutlineContinuationService(
                projectMapper,
                settingLibraryMapper,
                outlineMapper,
                volumeMapper,
                storyArcMapper,
                chapterMapper,
                chapterSummaryMapper,
                foreshadowThreadMapper,
                aiOrchestratorService,
                generationJobService,
                versionService,
                chapterConverter,
                transactionTemplate);
        ChapterOutlineContinueRequest request = new ChapterOutlineContinueRequest();
        request.setCount(50);
        request.setModelConfigId(7L);

        List<ChapterResponse> result = service.continueChapterOutlines(1L, request);

        assertThat(result).hasSize(50);
        assertThat(result).extracting(ChapterResponse::getChapterNo)
                .containsExactlyElementsOf(IntStream.rangeClosed(9, 58).boxed().toList());
        ArgumentCaptor<Map<String, Object>> contexts = ArgumentCaptor.forClass(Map.class);
        verify(aiOrchestratorService, times(5)).continueChapterOutline(eq(7L), contexts.capture());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secondBatchRecent =
                (List<Map<String, Object>>) contexts.getAllValues().get(1).get("recentChapterOutlines");
        assertThat(secondBatchRecent).extracting(item -> item.get("chapterNo"))
                .containsSequence(IntStream.rangeClosed(9, 18).boxed().toArray());
        var order = inOrder(aiOrchestratorService, transactionTemplate);
        order.verify(aiOrchestratorService, times(5)).continueChapterOutline(eq(7L), any());
        order.verify(transactionTemplate).execute(any(TransactionCallback.class));
        ArgumentCaptor<Chapter> inserted = ArgumentCaptor.forClass(Chapter.class);
        verify(chapterMapper, times(50)).insert(inserted.capture());
        assertThat(inserted.getAllValues()).extracting(Chapter::getChapterNo)
                .containsExactlyElementsOf(IntStream.rangeClosed(9, 58).boxed().toList());
        verify(chapterMapper, never()).delete(any());
    }
}
