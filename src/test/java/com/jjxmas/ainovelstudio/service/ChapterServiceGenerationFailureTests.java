package com.jjxmas.ainovelstudio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.ai.AiGenerateResult;
import com.jjxmas.ainovelstudio.ai.AiOrchestratorService;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterStreamEvent;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.service.impl.ChapterServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

class ChapterServiceGenerationFailureTests {

    private ChapterMapper chapterMapper;
    private OutlineMapper outlineMapper;
    private ProjectMapper projectMapper;
    private AiOrchestratorService aiOrchestratorService;
    private GenerationJobService generationJobService;
    private VersionService versionService;
    private ProjectChapterGenerationQueue generationQueue;
    private TransactionTemplate transactionTemplate;
    private ChapterServiceImpl service;

    @BeforeEach
    void setUp() {
        chapterMapper = mock(ChapterMapper.class);
        outlineMapper = mock(OutlineMapper.class);
        projectMapper = mock(ProjectMapper.class);
        aiOrchestratorService = mock(AiOrchestratorService.class);
        generationJobService = mock(GenerationJobService.class);
        versionService = mock(VersionService.class);
        generationQueue = mock(ProjectChapterGenerationQueue.class);
        transactionTemplate = mock(TransactionTemplate.class);
        service = new ChapterServiceImpl(
                projectMapper,
                outlineMapper,
                generationJobService,
                versionService,
                aiOrchestratorService,
                mock(ChapterContextAssembler.class),
                mock(ChapterPostProcessService.class),
                generationQueue,
                mock(ChapterConverter.class),
                transactionTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", chapterMapper);

        Outline outline = new Outline().setProjectId(1L).setConfirmedAt(LocalDateTime.now());
        when(outlineMapper.selectOne(any())).thenReturn(outline);
        when(projectMapper.lockById(1L)).thenReturn(1L);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void successfulGenerationUpdatesContentStateMetadata() {
        Chapter chapter = chapter(20L, null);
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(chapterMapper.selectByIdForUpdate(20L)).thenReturn(chapter);
        when(aiOrchestratorService.generateChapter(any(), any())).thenReturn(
                AiGenerateResult.builder().success(true).content("新正文").build());
        when(generationJobService.recordFinishedJob(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(88L);
        when(versionService.recordVersion(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(4);

        ChapterGenerateRequest request = new ChapterGenerateRequest();
        request.setProjectId(1L);
        request.setChapterId(20L);

        service.generateChapter(request);

        assertThat(chapter.getContent()).isEqualTo("新正文");
        assertThat(chapter.getContentStatus()).isEqualTo("generated");
        assertThat(chapter.getContentGeneratedAt()).isNotNull();
        assertThat(chapter.getContentUpdatedAt()).isNotNull();
        assertThat(chapter.getLastGenerationJobId()).isEqualTo(88L);
        assertThat(chapter.getLastContentVersionNo()).isEqualTo(4);
    }

    @Test
    void generationDoesNotOverwriteContentChangedWhileAiWasRunning() {
        Chapter original = chapter(20L, "旧正文").setLastContentVersionNo(2);
        Chapter current = chapter(20L, "用户新正文").setLastContentVersionNo(3);
        when(chapterMapper.selectById(20L)).thenReturn(original);
        when(chapterMapper.selectByIdForUpdate(20L)).thenReturn(current);
        when(aiOrchestratorService.generateChapter(any(), any())).thenReturn(
                AiGenerateResult.builder().success(true).content("过期的 AI 正文").build());

        ChapterGenerateRequest request = new ChapterGenerateRequest();
        request.setProjectId(1L);
        request.setChapterId(20L);

        assertThatThrownBy(() -> service.generateChapter(request))
                .hasMessage("CHAPTER_CONTENT_VERSION_CONFLICT");
        assertThat(current.getContent()).isEqualTo("用户新正文");
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        verify(generationJobService, never()).recordFinishedJob(any(), any(), any(), any(), any(), any(), any());
        verify(versionService, never()).recordVersion(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failedGenerationDoesNotOverwriteExistingContent() {
        Chapter chapter = chapter(20L, "旧正文");
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(aiOrchestratorService.generateChapter(any(), any())).thenReturn(
                AiGenerateResult.builder().success(false).content(null).build());

        ChapterGenerateRequest request = new ChapterGenerateRequest();
        request.setProjectId(1L);
        request.setChapterId(20L);

        assertThatThrownBy(() -> service.generateChapter(request))
                .hasMessage("模型未返回有效的章节正文");
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        org.assertj.core.api.Assertions.assertThat(chapter.getContent()).isEqualTo("旧正文");
    }

    @Test
    void failedRewriteDoesNotOverwriteExistingContent() {
        Chapter chapter = chapter(20L, "旧正文");
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(aiOrchestratorService.rewriteChapter(any(), any(), any())).thenReturn(
                AiGenerateResult.builder().success(false).content(" ").build());

        ChapterRewriteRequest request = new ChapterRewriteRequest();
        request.setModelConfigId(1L);
        request.setInstruction("加强冲突");

        assertThatThrownBy(() -> service.rewriteChapter(20L, request))
                .hasMessage("模型未返回有效的章节正文");
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        org.assertj.core.api.Assertions.assertThat(chapter.getContent()).isEqualTo("旧正文");
    }

    @Test
    void emptyStreamDoesNotPersistChapterContent() {
        Chapter chapter = chapter(20L, "旧正文");
        when(chapterMapper.selectById(20L)).thenReturn(chapter);
        when(aiOrchestratorService.streamChapter(any(), any())).thenReturn(Flux.empty());
        when(generationQueue.enqueue(anyLong(), any())).thenAnswer(invocation -> {
            Supplier<Flux<ChapterStreamEvent>> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        ChapterGenerateRequest request = new ChapterGenerateRequest();
        request.setProjectId(1L);
        request.setChapterId(20L);

        List<ChapterStreamEvent> events = service.streamGenerateChapter(request).collectList().block();

        org.assertj.core.api.Assertions.assertThat(events).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(events.get(events.size() - 1).getType()).isEqualTo("error");
        verify(chapterMapper, never()).updateById(any(Chapter.class));
        org.assertj.core.api.Assertions.assertThat(chapter.getContent()).isEqualTo("旧正文");
    }

    private Chapter chapter(Long id, String content) {
        Chapter chapter = new Chapter()
                .setProjectId(1L)
                .setChapterNo(1)
                .setTitle("第一章")
                .setOutline("章节大纲")
                .setContent(content);
        chapter.setId(id);
        return chapter;
    }
}
