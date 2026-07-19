package com.jjxmas.ainovelstudio.ai;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestratorService {

    private final NovelAiClient novelAiClient;
    private final PromptTemplateService promptTemplateService;

    public AiOrchestratorService(NovelAiClient novelAiClient, PromptTemplateService promptTemplateService) {
        this.novelAiClient = novelAiClient;
        this.promptTemplateService = promptTemplateService;
    }

    public AiGenerateResult generateChapter(
            Long modelConfigId,
            Map<String, Object> context,
            String title,
            String outline,
            String advice) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_GENERATION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_GENERATION))
                .userPrompt(promptTemplateService.chapterGenerationPrompt(context, title, outline, advice))
                .context(context)
                .temperature(0.75)
                .build());
    }

    public AiGenerateResult rewriteChapter(
            Long modelConfigId,
            Map<String, Object> context,
            String content,
            String instruction) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.REWRITE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.REWRITE))
                .userPrompt(promptTemplateService.rewritePrompt(context, content, instruction))
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
}
