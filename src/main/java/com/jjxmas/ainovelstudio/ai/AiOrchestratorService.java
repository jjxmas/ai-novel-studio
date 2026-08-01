package com.jjxmas.ainovelstudio.ai;

import java.util.Map;

import com.jjxmas.ainovelstudio.prompts.IdeaGenerationPrompts;
import org.springframework.stereotype.Service;

@Service
/**
 * AI 编排服务，负责把业务任务转换成统一的模型调用命令。
 */
public class AiOrchestratorService {

    private final NovelAiClient novelAiClient;
    private final PromptTemplateService promptTemplateService;

    /**
     * 注入模型客户端和提示词模板服务。
     */
    public AiOrchestratorService(NovelAiClient novelAiClient, PromptTemplateService promptTemplateService) {
        this.novelAiClient = novelAiClient;
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 生成章节正文。
     */
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

    /**
     * 生成单个创意候选方案。
     */
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

    /**
     * 大模型评估创意。
     */
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

    /**
     * 根据修改指令重写创意。
     */
    public AiGenerateResult rewriteIdea(Long modelConfigId, String original, String instruction) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.REWRITE)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.REWRITE))
                .userPrompt(promptTemplateService.ideaRewritePrompt(original, instruction))
                .temperature(0.75)
                .build());
    }

    /**
     * 根据上下文和修改指令重写章节正文。
     */
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

    /**
     * 为章节正文生成单章摘要。
     */
    public AiGenerateResult summarizeChapter(Long modelConfigId, String title, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.CHAPTER_SUMMARY)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.CHAPTER_SUMMARY))
                .userPrompt(promptTemplateService.chapterSummaryPrompt(title, content))
                .temperature(0.3)
                .build());
    }

    /**
     * 将多条摘要压缩为分层记忆。
     */
    public AiGenerateResult compressMemory(Long modelConfigId, String sourceType, String content) {
        return novelAiClient.generate(AiGenerateCommand.builder()
                .modelConfigId(modelConfigId)
                .taskType(AiTaskType.MEMORY_COMPRESSION)
                .systemPrompt(promptTemplateService.systemPrompt(AiTaskType.MEMORY_COMPRESSION))
                .userPrompt(promptTemplateService.compressionPrompt(sourceType, content))
                .temperature(0.3)
                .build());
    }

    /**
     * 用新增阶段记忆更新全局记忆。
     */
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
