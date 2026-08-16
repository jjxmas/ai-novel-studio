package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.converter.VersionConverter;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.service.impl.VersionServiceImpl;
import java.util.Map;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class VersionServiceTests {

    private ContentVersionMapper contentVersionMapper;
    private ProjectMapper projectMapper;
    private VersionServiceImpl service;

    @BeforeEach
    void setUp() {
        contentVersionMapper = mock(ContentVersionMapper.class);
        projectMapper = mock(ProjectMapper.class);
        service = new VersionServiceImpl(contentVersionMapper, projectMapper, mock(VersionConverter.class));
        when(projectMapper.lockById(1L)).thenReturn(1L);
    }

    @Test
    void locksProjectBeforeAllocatingAndReturnsNextVersionNumber() {
        ContentVersion latest = new ContentVersion().setVersionNo(7);
        when(contentVersionMapper.selectOne(any())).thenReturn(latest);

        int versionNo = service.recordVersion(
                1L, "chapter", 20L, Map.of("content", "正文"), "user_edit", "修改", null, null);

        assertThat(versionNo).isEqualTo(8);
        ArgumentCaptor<ContentVersion> inserted = ArgumentCaptor.forClass(ContentVersion.class);
        verify(contentVersionMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getVersionNo()).isEqualTo(8);
        InOrder order = inOrder(projectMapper, contentVersionMapper);
        order.verify(projectMapper).lockById(1L);
        order.verify(contentVersionMapper).selectOne(any());
        order.verify(contentVersionMapper).insert(any(ContentVersion.class));
    }

    @Test
    void startsAtVersionOneWhenEntityHasNoHistory() {
        when(contentVersionMapper.selectOne(any())).thenReturn(null);

        int versionNo = service.recordVersion(
                1L, "chapter", 20L, Map.of("content", "正文"), "ai_generate", "生成", 3L, 9L);

        assertThat(versionNo).isEqualTo(1);
    }

    @Test
    void rejectsVersionForMissingProject() {
        when(projectMapper.lockById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.recordVersion(
                1L, "chapter", 20L, Map.of(), "user_edit", null, null, null))
                .hasMessage("作品不存在");
        verify(contentVersionMapper, never()).selectOne(any());
        verify(contentVersionMapper, never()).insert(any(ContentVersion.class));
    }

    @Test
    void versionResponseContainsAuthoritativeMetadata() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 16, 12, 30);
        ContentVersion version = new ContentVersion()
                .setProjectId(1L)
                .setEntityType("chapter")
                .setEntityId(20L)
                .setVersionNo(8)
                .setChangeSource("user_edit")
                .setOperationType("edit")
                .setChangeNote("修改正文")
                .setRevisionInstruction("压缩对话")
                .setModelConfigId(3L)
                .setJobId(9L);
        version.setId(88L);
        version.setCreatedAt(createdAt);

        var response = Mappers.getMapper(VersionConverter.class).toResponse(version);

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getVersionNo()).isEqualTo(8);
        assertThat(response.getOperationType()).isEqualTo("edit");
        assertThat(response.getRevisionInstruction()).isEqualTo("压缩对话");
        assertThat(response.getModelConfigId()).isEqualTo(3L);
        assertThat(response.getJobId()).isEqualTo(9L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }
}
