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
import com.jjxmas.ainovelstudio.converter.IdeaConverter;
import com.jjxmas.ainovelstudio.mapper.IdeaEvaluationMapper;
import com.jjxmas.ainovelstudio.mapper.IdeaMapper;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaResponse;
import com.jjxmas.ainovelstudio.pojo.dto.IdeaRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Idea;
import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.service.impl.IdeaServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class IdeaServiceRewriteTests {

    private IdeaMapper ideaMapper;
    private IdeaEvaluationMapper evaluationMapper;
    private ProjectMapper projectMapper;
    private ModelConfigMapper modelConfigMapper;
    private GenerationJobService generationJobService;
    private VersionService versionService;
    private AiOrchestratorService aiOrchestratorService;
    private IdeaConverter ideaConverter;
    private TransactionTemplate transactionTemplate;
    private IdeaServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ideaMapper = mock(IdeaMapper.class);
        evaluationMapper = mock(IdeaEvaluationMapper.class);
        projectMapper = mock(ProjectMapper.class);
        modelConfigMapper = mock(ModelConfigMapper.class);
        generationJobService = mock(GenerationJobService.class);
        versionService = mock(VersionService.class);
        aiOrchestratorService = mock(AiOrchestratorService.class);
        ideaConverter = mock(IdeaConverter.class);
        transactionTemplate = mock(TransactionTemplate.class);
        service = new IdeaServiceImpl(
                evaluationMapper,
                projectMapper,
                generationJobService,
                versionService,
                aiOrchestratorService,
                modelConfigMapper,
                transactionTemplate,
                ideaConverter);
        ReflectionTestUtils.setField(service, "baseMapper", ideaMapper);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<IdeaResponse>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        when(evaluationMapper.selectOne(any())).thenReturn(null);
        when(ideaConverter.toResponse(any(), any())).thenAnswer(invocation -> {
            Idea idea = invocation.getArgument(0);
            return IdeaResponse.builder().id(idea.getId()).title(idea.getTitle()).summary(idea.getSummary()).build();
        });
    }

    @Test
    void rewriteCallsAiBeforeTransactionAndPersistsOneVersion() {
        Idea idea = idea("原标题");
        when(ideaMapper.selectById(10L)).thenReturn(idea);
        when(ideaMapper.selectByIdForUpdate(10L)).thenReturn(idea("原标题"));
        when(aiOrchestratorService.rewriteIdea(any(), any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("标题：新标题\n世界观：新世界\n主线冲突：新冲突\n正文")
                .modelName("test-model")
                .build());
        when(generationJobService.recordFinishedJob(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(20L);

        IdeaResponse response = service.rewriteIdea(10L, request());

        assertThat(response.getTitle()).isEqualTo("新标题");
        var order = inOrder(aiOrchestratorService, transactionTemplate);
        order.verify(aiOrchestratorService).rewriteIdea(any(), any(), any());
        order.verify(transactionTemplate).execute(any(TransactionCallback.class));
        verify(ideaMapper).updateById(any(Idea.class));
        verify(versionService).recordVersion(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rewriteRejectsResultWhenIdeaChangedDuringAiCall() {
        when(ideaMapper.selectById(10L)).thenReturn(idea("原标题"));
        when(ideaMapper.selectByIdForUpdate(10L)).thenReturn(idea("用户刚刚修改的标题"));
        when(aiOrchestratorService.rewriteIdea(any(), any(), any())).thenReturn(
                AiGenerateResult.builder().success(true).content("标题：AI 标题\n正文").build());

        assertThatThrownBy(() -> service.rewriteIdea(10L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("创意已被修改");
        verify(ideaMapper, never()).updateById(any(Idea.class));
        verifyNoInteractions(generationJobService, versionService);
    }

    @Test
    void generationDoesNotPersistIdeaWhenEvaluationFails() {
        Project project = new Project().setTitle("测试作品").setGenres(List.of("玄幻"));
        project.setId(1L);
        ModelConfig modelConfig = new ModelConfig().setUsageType("idea_generation").setEnabled(true);
        modelConfig.setId(3L);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(modelConfigMapper.selectOne(any())).thenReturn(modelConfig);
        when(aiOrchestratorService.generateIdea(any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("{\"title\":\"候选创意\",\"sellingPoints\":[\"卖点\"],"
                        + "\"worldview\":\"世界\",\"mainConflict\":\"冲突\","
                        + "\"estimatedWordCount\":1500000,\"summary\":\"摘要\"}")
                .build());
        when(aiOrchestratorService.evaluateIdea(any(), any(), any())).thenReturn(null);
        IdeaGenerateRequest request = new IdeaGenerateRequest();
        request.setProjectId(1L);
        request.setModelType("idea_generation");
        request.setBriefDescription("生成一个长篇创意");
        request.setIdeaCount(1);

        List<IdeaResponse> result = service.generateIdeas(request);

        assertThat(result).isEmpty();
        verify(ideaMapper, never()).insert(any(Idea.class));
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    private Idea idea(String title) {
        Idea idea = new Idea()
                .setProjectId(1L)
                .setTitle(title)
                .setSellingPoints(List.of("卖点"))
                .setWorldview("旧世界")
                .setMainConflict("旧冲突")
                .setEstimatedWordCount(1_500_000)
                .setSummary("旧摘要")
                .setStatus("candidate");
        idea.setId(10L);
        return idea;
    }

    private IdeaRewriteRequest request() {
        IdeaRewriteRequest request = new IdeaRewriteRequest();
        request.setModelConfigId(3L);
        request.setInstruction("增强开篇卖点");
        return request;
    }
}
