package com.jjxmas.ainovelstudio.ai;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.prompts.IdeaGenerationPrompts;
import java.util.Map;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestratorService {

    private final NovelAiClient novelAiClient;
    private final PromptTemplateService promptTemplateService;

    public AiOrchestratorService(NovelAiClient novelAiClient, PromptTemplateService promptTemplateService) {
        this.novelAiClient = novelAiClient;
        this.promptTemplateService = promptTemplateService;
    }

    public AiGenerateResult generateChapter(Long modelConfigId, ChapterContext context) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_GENERATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_GENERATION))
                .userPrompt(promptTemplateService.chapterGenerationPrompt(context))
                .context(context)
                .temperature(0.75)
                .build());
    }

    public Flux<String> streamChapter(Long modelConfigId, ChapterContext context) {
        return novelAiClient.stream(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_GENERATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_GENERATION))
                .userPrompt(promptTemplateService.chapterGenerationPrompt(context))
                .context(context)
                .temperature(0.75)
                .build());
    }

    public AiGenerateResult generateIdea(Long modelConfigId, Map<String, Object> context) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.IDEA_GENERATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.IDEA_GENERATION))
                .userPrompt(IdeaGenerationPrompts.ideaGenerationPrompt(context))
                .context(context)
                .temperature(0.92)
                .build());
    }

    public AiGenerateResult generateSettingBlueprint(Long modelConfigId, Map<String, Object> context) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.SETTING_BLUEPRINT)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.SETTING_BLUEPRINT))
                .userPrompt(promptTemplateService.settingBlueprintPrompt(context))
                .context(context)
                .temperature(0.55)
                .build());
    }

    public AiGenerateResult generateSettingDraft(Long modelConfigId, Map<String, Object> context, Object blueprint) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.SETTING_DRAFT)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.SETTING_DRAFT))
                .userPrompt(promptTemplateService.settingDraftPrompt(context, blueprint))
                .context(context)
                .temperature(0.65)
                .build());
    }

    public AiGenerateResult generateOutlineWorkflowDraft(Long modelConfigId, Map<String, Object> context) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.OUTLINE_WORKFLOW_DRAFT)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.OUTLINE_WORKFLOW_DRAFT))
                .userPrompt(promptTemplateService.outlineWorkflowDraftPrompt(context))
                .context(context)
                .temperature(0.6)
                .build());
    }

    public AiGenerateResult continueChapterOutline(Long modelConfigId, Map<String, Object> context) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_OUTLINE_CONTINUATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_OUTLINE_CONTINUATION))
                .userPrompt(promptTemplateService.chapterOutlineContinuationPrompt(context))
                .context(context)
                .temperature(0.55)
                .build());
    }

    public AiGenerateResult evaluateIdea(Long modelConfigId, Map<String, Object> context, Map<String, Object> idea) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.IDEA_EVALUATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.IDEA_EVALUATION))
                .userPrompt(IdeaGenerationPrompts.ideaEvaluationPrompt(context, idea))
                .context(context)
                .temperature(0.2)
                .build());
    }

    public AiGenerateResult rewriteIdea(Long modelConfigId, String original, String instruction) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.REWRITE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.REWRITE))
                .userPrompt(promptTemplateService.ideaRewritePrompt(original, instruction))
                .temperature(0.75)
                .build());
    }

    public AiGenerateResult rewriteChapter(Long modelConfigId, ChapterContext context, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.REWRITE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.REWRITE))
                .userPrompt(promptTemplateService.rewritePrompt(context, content))
                .context(context)
                .temperature(0.7)
                .build());
    }

    public Flux<String> streamRewriteChapter(Long modelConfigId, ChapterContext context, String content) {
        return novelAiClient.stream(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.REWRITE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.REWRITE))
                .userPrompt(promptTemplateService.rewritePrompt(context, content))
                .context(context)
                .temperature(0.7)
                .build());
    }

    public AiGenerateResult summarizeChapter(Long modelConfigId, String title, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_SUMMARY)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_SUMMARY))
                .userPrompt(promptTemplateService.chapterSummaryPrompt(title, content))
                .temperature(0.3)
                .build());
    }

    public AiGenerateResult extractChapterFacts(Long modelConfigId, String title, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_FACT_EXTRACTION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_FACT_EXTRACTION))
                .userPrompt(promptTemplateService.chapterFactExtractionPrompt(title, content))
                .temperature(0.2)
                .build());
    }

    public AiGenerateResult compressMemory(Long modelConfigId, String sourceType, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.MEMORY_COMPRESSION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.MEMORY_COMPRESSION))
                .userPrompt(promptTemplateService.compressionPrompt(sourceType, content))
                .temperature(0.3)
                .build());
    }

    public AiGenerateResult updateGlobalMemory(Long modelConfigId, String oldGlobal, String newMemory) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.GLOBAL_MEMORY_UPDATE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.GLOBAL_MEMORY_UPDATE))
                .userPrompt(promptTemplateService.globalMemoryPrompt(oldGlobal, newMemory))
                .temperature(0.2)
                .build());
    }

    public AiGenerateResult checkChapter(
            Long modelConfigId,
            String checkType,
            Map<String, Object> context) {
        AiTaskType taskType = "style".equals(checkType) ? AiTaskType.STYLE_CHECK : AiTaskType.CONTINUITY_CHECK;
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(taskType)
                .systemPrompt(promptTemplateService.systemPrompt(taskType))
                .userPrompt(promptTemplateService.qualityCheckPrompt(checkType, context))
                .context(context)
                .temperature(0.15)
                .build());
    }
}
