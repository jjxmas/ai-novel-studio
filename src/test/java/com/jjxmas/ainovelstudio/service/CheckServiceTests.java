package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.CheckResultMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.CheckRequest;
import com.jjxmas.ainovelstudio.pojo.dto.CheckResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.CheckResult;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.service.impl.CheckServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CheckServiceTests {

    private ProjectMapper projectMapper;
    private ChapterMapper chapterMapper;
    private CheckResultMapper checkResultMapper;
    private GenerationJobService generationJobService;
    private AiOrchestratorService aiOrchestratorService;
    private TransactionTemplate transactionTemplate;
    private CheckServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), CheckResult.class);
        projectMapper = mock(ProjectMapper.class);
        chapterMapper = mock(ChapterMapper.class);
        checkResultMapper = mock(CheckResultMapper.class);
        generationJobService = mock(GenerationJobService.class);
        aiOrchestratorService = mock(AiOrchestratorService.class);
        transactionTemplate = mock(TransactionTemplate.class);
        service = new CheckServiceImpl(
                projectMapper,
                chapterMapper,
                generationJobService,
                aiOrchestratorService,
                new ObjectMapper(),
                transactionTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", checkResultMapper);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<CheckResponse>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(generationJobService.recordFinishedJob(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(30L);
    }

    @Test
    void checksWithAiBeforeTransactionAndPersistsAuthoritativeResult() {
        Chapter chapter = chapter("当前正文");
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(chapterMapper.selectOne(any())).thenReturn(null);
        when(chapterMapper.selectByIdForUpdate(20L)).thenReturn(chapter("当前正文"));
        when(aiOrchestratorService.checkChapter(any(), any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .modelName("quality-model")
                .content("""
                        {"summary":"发现一项承接问题","issues":[{
                          "type":"continuity","severity":"medium",
                          "description":"开场地点与上一章不一致",
                          "suggestion":"补充移动过程","reference":"开场第一段"
                        }]}
                        """)
                .build());

        CheckResponse response = service.runCheck(request());

        assertThat(response.getIssueCount()).isEqualTo(1);
        var order = inOrder(aiOrchestratorService, transactionTemplate);
        order.verify(aiOrchestratorService).checkChapter(any(), any(), any());
        order.verify(transactionTemplate).execute(any(TransactionCallback.class));
        ArgumentCaptor<CheckResult> resultCaptor = ArgumentCaptor.forClass(CheckResult.class);
        verify(checkResultMapper).insert(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCheckType()).isEqualTo("continuity:continuity");
        assertThat(resultCaptor.getValue().getChapterId()).isEqualTo(20L);
        verify(checkResultMapper).update(isNull(), any());
        verify(chapterMapper).updateById(any(Chapter.class));
    }

    @Test
    void invalidAiResponseDoesNotStartPersistenceTransaction() {
        when(chapterMapper.selectById(20L)).thenReturn(chapter("当前正文"));
        when(chapterMapper.selectOne(any())).thenReturn(null);
        when(aiOrchestratorService.checkChapter(any(), any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("不是 JSON")
                .build());

        assertThatThrownBy(() -> service.runCheck(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI_CHECK_RESPONSE_INVALID");

        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
        verify(checkResultMapper, never()).insert(any(CheckResult.class));
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        verify(generationJobService).recordFailedJob(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsAiResultWhenChapterChangesDuringCheck() {
        when(chapterMapper.selectById(20L)).thenReturn(chapter("检查前正文"));
        when(chapterMapper.selectOne(any())).thenReturn(null);
        when(chapterMapper.selectByIdForUpdate(20L)).thenReturn(chapter("用户刚修改的正文"));
        when(aiOrchestratorService.checkChapter(any(), any(), any())).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("{\"summary\":\"无问题\",\"issues\":[]}")
                .build());

        assertThatThrownBy(() -> service.runCheck(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("章节正文已被修改");

        verify(checkResultMapper, never()).insert(any(CheckResult.class));
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        verify(generationJobService, never()).recordFinishedJob(any(), any(), any(), any(), any(), any(), any());
    }

    private CheckRequest request() {
        CheckRequest request = new CheckRequest();
        request.setProjectId(1L);
        request.setChapterId(20L);
        request.setModelConfigId(7L);
        request.setCheckType("continuity");
        return request;
    }

    private Chapter chapter(String content) {
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(2)
                .setTitle("第二章")
                .setOutline("承接上一章")
                .setContent(content)
                .setLastContentVersionNo(3);
        chapter.setId(20L);
        return chapter;
    }
}
