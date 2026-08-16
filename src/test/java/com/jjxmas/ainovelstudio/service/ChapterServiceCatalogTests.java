package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterCatalogResponse;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.service.impl.ChapterServiceImpl;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class ChapterServiceCatalogTests {

    private ChapterMapper chapterMapper;
    private ProjectMapper projectMapper;
    private ChapterConverter chapterConverter;
    private ChapterServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), Chapter.class);
        chapterMapper = mock(ChapterMapper.class);
        projectMapper = mock(ProjectMapper.class);
        chapterConverter = mock(ChapterConverter.class);
        service = new ChapterServiceImpl(
                projectMapper,
                mock(OutlineMapper.class),
                mock(GenerationJobService.class),
                mock(VersionService.class),
                mock(AiOrchestratorService.class),
                mock(ChapterContextAssembler.class),
                mock(ChapterPostProcessService.class),
                mock(ProjectChapterGenerationQueue.class),
                chapterConverter,
                mock(TransactionTemplate.class));
        ReflectionTestUtils.setField(service, "baseMapper", chapterMapper);
        when(projectMapper.selectById(1L)).thenReturn(new Project());
    }

    @Test
    void pagedChapterQuerySelectsMetadataWithoutContent() {
        Chapter chapter = chapter();
        ChapterCatalogResponse catalog = ChapterCatalogResponse.builder().id(20L).hasContent(true).build();
        when(chapterMapper.selectCount(any())).thenReturn(120L);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        when(chapterConverter.toCatalogResponseList(List.of(chapter))).thenReturn(List.of(catalog));

        var response = service.listChapters(1L, "12", 2, 20);

        assertThat(response.getTotal()).isEqualTo(120L);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getItems()).containsExactly(catalog);
        ArgumentCaptor<Wrapper<Chapter>> queryCaptor = wrapperCaptor();
        verify(chapterMapper).selectList(queryCaptor.capture());
        assertMetadataOnly(queryCaptor.getValue());
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("LIMIT 20 OFFSET 20");
    }

    @Test
    void catalogQuerySelectsMetadataWithoutContent() {
        Chapter chapter = chapter();
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        when(chapterConverter.toCatalogResponseList(any())).thenReturn(List.of());

        service.listChapterCatalog(1L);

        ArgumentCaptor<Wrapper<Chapter>> queryCaptor = wrapperCaptor();
        verify(chapterMapper).selectList(queryCaptor.capture());
        assertMetadataOnly(queryCaptor.getValue());
    }

    @Test
    void detailReturnsFullChapterAndPageSizeIsBounded() {
        Chapter chapter = chapter().setContent("完整正文");
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(chapterConverter.toResponse(chapter)).thenReturn(ChapterResponse.builder()
                .id(20L)
                .content("完整正文")
                .build());

        assertThat(service.getChapter(20L).getContent()).isEqualTo("完整正文");
        assertThatThrownBy(() -> service.listChapters(1L, "", 1, 101))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分页参数无效");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Wrapper<Chapter>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

    private void assertMetadataOnly(Wrapper<Chapter> wrapper) {
        assertThat(Arrays.stream(wrapper.getSqlSelect().split(","))
                .map(String::trim))
                .doesNotContain("content")
                .contains("contentStatus", "lastContentVersionNo");
    }

    private Chapter chapter() {
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(12)
                .setTitle("第十二章")
                .setOutline("章节大纲")
                .setContentStatus("generated")
                .setLastContentVersionNo(3);
        chapter.setId(20L);
        return chapter;
    }
}
