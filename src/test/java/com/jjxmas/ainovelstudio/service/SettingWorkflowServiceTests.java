package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.converter.SettingWorkflowConverter;
import com.jjxmas.ainovelstudio.mapper.EntityRelationMapper;
import com.jjxmas.ainovelstudio.mapper.EntityStateRecordMapper;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.mapper.OrganizationMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.SettingWorkflowRunMapper;
import com.jjxmas.ainovelstudio.mapper.StoryCharacterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryEventMapper;
import com.jjxmas.ainovelstudio.mapper.StoryItemMapper;
import com.jjxmas.ainovelstudio.mapper.StoryLocationMapper;
import com.jjxmas.ainovelstudio.mapper.WorldRuleMapper;
import com.jjxmas.ainovelstudio.pojo.dto.SettingLibraryResponse;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowCreateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.SettingWorkflowResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.SettingWorkflowRun;
import com.jjxmas.ainovelstudio.service.impl.SettingWorkflowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class SettingWorkflowServiceTests {

    @Test
    void commitUsesCanonicalSettingDraftPathWithoutCreatingAnotherGenerationJob() {
        SettingWorkflowRunMapper runMapper = mock(SettingWorkflowRunMapper.class);
        SettingLibraryService settingLibraryService = mock(SettingLibraryService.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        SettingWorkflowRun run = new SettingWorkflowRun()
                .setProjectId(1L)
                .setSourceIdeaId(2L)
                .setModelConfigId(3L)
                .setStatus("draft_ready")
                .setDraftJson("{\"overview\":\"设定总览\"}");
        run.setId(4L);
        SettingLibraryResponse expected = SettingLibraryResponse.builder().id(5L).projectId(1L).build();
        when(runMapper.selectOne(any())).thenReturn(run);
        when(settingLibraryService.commitWorkflowDraft(1L, 2L, "设定总览", 3L, 4L)).thenReturn(expected);
        SettingWorkflowService service = service(runMapper, mock(ProjectMapper.class), mock(IdeaMapper.class),
                settingLibraryService, generationJobService, mock(AiOrchestratorService.class),
                mock(SettingWorkflowConverter.class), mock(TransactionTemplate.class));

        SettingLibraryResponse actual = service.commitWorkflow(4L);

        assertThat(actual).isSameAs(expected);
        assertThat(run.getStatus()).isEqualTo("committed");
        assertThat(run.getCommittedAt()).isNotNull();
        verify(settingLibraryService).commitWorkflowDraft(1L, 2L, "设定总览", 3L, 4L);
        verify(runMapper).updateById(run);
        verifyNoInteractions(generationJobService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void startWorkflowCallsAiBeforeOpeningWriteTransaction() {
        SettingWorkflowRunMapper runMapper = mock(SettingWorkflowRunMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        IdeaMapper ideaMapper = mock(IdeaMapper.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
        SettingWorkflowConverter converter = mock(SettingWorkflowConverter.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        Project project = new Project().setSelectedIdeaId(2L);
        project.setId(1L);
        Idea idea = new Idea().setProjectId(1L).setTitle("创意");
        idea.setId(2L);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(ideaMapper.selectById(2L)).thenReturn(idea);
        when(aiOrchestratorService.generateSettingBlueprint(any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("{\"corePremise\":\"前提\",\"mainConflict\":\"冲突\",\"entities\":{"
                        + "\"characters\":[{}],\"organizations\":[{}],\"locations\":[{}],"
                        + "\"items\":[{}],\"events\":[{}]}}")
                .build());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<SettingWorkflowResponse>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<SettingWorkflowRun>getArgument(0).setId(4L);
            return 1;
        }).when(runMapper).insert(any(SettingWorkflowRun.class));
        when(converter.toResponse(any())).thenAnswer(invocation -> {
            SettingWorkflowRun run = invocation.getArgument(0);
            return SettingWorkflowResponse.builder().id(run.getId()).projectId(run.getProjectId()).build();
        });
        SettingWorkflowService service = service(runMapper, projectMapper, ideaMapper,
                mock(SettingLibraryService.class), generationJobService, aiOrchestratorService, converter,
                transactionTemplate);
        SettingWorkflowCreateRequest request = new SettingWorkflowCreateRequest();
        request.setProjectId(1L);
        request.setIdeaId(2L);
        request.setModelConfigId(3L);

        SettingWorkflowResponse response = service.startWorkflow(request);

        assertThat(response.getId()).isEqualTo(4L);
        var order = inOrder(aiOrchestratorService, transactionTemplate);
        order.verify(aiOrchestratorService).generateSettingBlueprint(any(), any());
        order.verify(transactionTemplate).execute(any(TransactionCallback.class));
        verify(generationJobService).recordFinishedJob(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void approveRejectsAiResultWhenWorkflowChangedDuringCall() {
        SettingWorkflowRunMapper runMapper = mock(SettingWorkflowRunMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        IdeaMapper ideaMapper = mock(IdeaMapper.class);
        GenerationJobService generationJobService = mock(GenerationJobService.class);
        AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        Project project = new Project().setSelectedIdeaId(2L);
        project.setId(1L);
        Idea idea = new Idea().setProjectId(1L);
        idea.setId(2L);
        SettingWorkflowRun snapshot = new SettingWorkflowRun()
                .setProjectId(1L).setSourceIdeaId(2L).setModelConfigId(3L)
                .setStatus("blueprint_ready").setBlueprintJson("{\"version\":1}");
        snapshot.setId(4L);
        SettingWorkflowRun changed = new SettingWorkflowRun()
                .setProjectId(1L).setSourceIdeaId(2L).setModelConfigId(3L)
                .setStatus("draft_ready").setBlueprintJson("{\"version\":1}");
        changed.setId(4L);
        when(runMapper.selectById(4L)).thenReturn(snapshot);
        when(runMapper.selectOne(any())).thenReturn(changed);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(ideaMapper.selectById(2L)).thenReturn(idea);
        when(aiOrchestratorService.generateSettingDraft(any(), any(), any())).thenReturn(
                AiGenerateResult.builder().success(true).content("{\"overview\":\"新草案\"}").build());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<SettingWorkflowResponse>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        SettingWorkflowService service = service(runMapper, projectMapper, ideaMapper,
                mock(SettingLibraryService.class), generationJobService, aiOrchestratorService,
                mock(SettingWorkflowConverter.class), transactionTemplate);

        assertThatThrownBy(() -> service.approveBlueprint(4L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工作流已变化");
        verify(runMapper, never()).updateById(any(SettingWorkflowRun.class));
        verifyNoInteractions(generationJobService);
    }

    private SettingWorkflowService service(
            SettingWorkflowRunMapper runMapper,
            ProjectMapper projectMapper,
            IdeaMapper ideaMapper,
            SettingLibraryService settingLibraryService,
            GenerationJobService generationJobService,
            AiOrchestratorService aiOrchestratorService,
            SettingWorkflowConverter converter,
            TransactionTemplate transactionTemplate) {
        return new SettingWorkflowServiceImpl(
                runMapper,
                projectMapper,
                ideaMapper,
                mock(StoryCharacterMapper.class),
                mock(OrganizationMapper.class),
                mock(StoryLocationMapper.class),
                mock(StoryItemMapper.class),
                mock(WorldRuleMapper.class),
                mock(EntityRelationMapper.class),
                mock(StoryEventMapper.class),
                mock(EntityStateRecordMapper.class),
                settingLibraryService,
                generationJobService,
                aiOrchestratorService,
                converter,
                transactionTemplate);
    }
}
