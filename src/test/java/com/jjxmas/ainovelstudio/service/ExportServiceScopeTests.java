package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ExportRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ExportResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.service.impl.ExportServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

class ExportServiceScopeTests {

    @Test
    void chapterScopeFiltersByChapterId() {
        TestContext context = contextWithChapter();
        ExportRequest request = request("chapter", 20L);

        ExportResponse response = context.service().exportProject(request);

        assertThat(response.getScope()).isEqualTo("chapter");
        assertThat(context.project().getWorkflowStage()).isEqualTo("export");
        assertThat(context.project().getLastExportedAt()).isNotNull();
        assertThat(capturedSql(context.chapterMapper())).contains("project_id", "id");
    }

    @Test
    void volumeScopeFiltersByVolumeId() {
        TestContext context = contextWithChapter();
        ExportRequest request = request("volume", 30L);

        ExportResponse response = context.service().exportProject(request);

        assertThat(response.getScope()).isEqualTo("volume");
        assertThat(capturedSql(context.chapterMapper())).contains("project_id", "volume_id");
    }

    @Test
    void rangedScopeRequiresEntityId() {
        TestContext context = contextWithChapter();

        assertThatThrownBy(() -> context.service().exportProject(request("chapter", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("SCOPE_ENTITY_ID_REQUIRED");
    }

    private TestContext contextWithChapter() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        Project project = new Project().setTitle("Test Novel");
        project.setId(10L);
        Chapter chapter = new Chapter()
                .setProjectId(10L)
                .setVolumeId(30L)
                .setChapterNo(1)
                .setTitle("Opening")
                .setContent("Content");
        chapter.setId(20L);
        when(projectMapper.selectById(10L)).thenReturn(project);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        return new TestContext(
                new ExportServiceImpl(projectMapper, chapterMapper, mock(VersionService.class)),
                chapterMapper,
                project);
    }

    @SuppressWarnings("unchecked")
    private String capturedSql(ChapterMapper chapterMapper) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Chapter.class);
        ArgumentCaptor<LambdaQueryWrapper<Chapter>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(chapterMapper).selectList(captor.capture());
        return captor.getValue().getSqlSegment();
    }

    private ExportRequest request(String scope, Long scopeEntityId) {
        ExportRequest request = new ExportRequest();
        request.setProjectId(10L);
        request.setScope(scope);
        request.setFormat("txt");
        request.setScopeEntityId(scopeEntityId);
        return request;
    }

    private record TestContext(ExportServiceImpl service, ChapterMapper chapterMapper, Project project) {
    }
}
