package com.jjxmas.ainovelstudio.ai;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    public String systemPrompt(AiTaskType taskType) {
        return switch (taskType) {
            case IDEA_GENERATION -> "你是长篇网文创意策划助手。请生成适合新手持续写作的长篇小说创意，输出中文内容。";
            case CHAPTER_GENERATION -> "你是长篇网文写作助手。请遵守已确认设定和大纲，只输出章节正文，不解释过程。";
            case REWRITE -> "你是长篇网文改写助手。请根据用户修改意见重写内容，保持原目标和关键设定一致。";
            case CHAPTER_SUMMARY -> "你是小说章节摘要助手。请提取剧情、人物状态、地点移动和伏笔变化，输出中文摘要。";
            case MEMORY_COMPRESSION -> "你是长篇小说记忆压缩助手。请把多条摘要压缩成一条中高层记忆，保留主线、人物变化和伏笔。";
            case GLOBAL_MEMORY_UPDATE -> "你是长篇小说总摘要维护助手。请根据旧总摘要和新阶段摘要更新全局总摘要。";
            default -> "你是小说创作助手。请按用户要求输出中文内容。";
        };
    }

    public String ideaGenerationPrompt(Map<String, Object> context, int index) {
        return """
                请生成第 %d 个长篇小说创意方案。

                【作品输入】
                %s

                输出要求：
                1. 包含标题、卖点、世界观、主线冲突、预估字数、风险提示。
                2. 适合新手按阶段扩写，不要只给一句梗概。
                3. 重点考虑长篇承载力、人物目标、平台连载节奏。
                4. 只输出这个方案本身，不要解释生成过程。
                """.formatted(index, context);
    }

    public String ideaRewritePrompt(String original, String instruction) {
        return """
                请根据修改意见重写这个小说创意方案。

                【原创意】
                %s

                【修改意见】
                %s

                要求：保留长篇承载力，强化卖点、世界观和主线冲突，只输出重写后的创意方案。
                """.formatted(original, instruction);
    }

    public String chapterGenerationPrompt(Map<String, Object> context, String title, String outline, String advice) {
        return """
                请生成一章适合连载网文的正文。

                【上下文】
                %s

                【章节标题】
                %s

                【章节大纲】
                %s

                【用户要求】
                %s

                要求：
                1. 不要写解释，不要输出大纲。
                2. 保持人物状态、时间线、地点移动和设定一致。
                3. 尽量减少机械总结感，多用具体动作、对话和场景细节。
                4. 结尾保留自然钩子。
                """.formatted(context, title, outline, blankToDefault(advice, "无"));
    }

    public String rewritePrompt(Map<String, Object> context, String content, String instruction) {
        return """
                请根据修改意见重写下面的章节正文。

                【上下文】
                %s

                【原正文】
                %s

                【修改意见】
                %s

                要求：只输出重写后的正文，保持连续性和风格一致。
                """.formatted(context, content, instruction);
    }

    public String chapterSummaryPrompt(String title, String content) {
        return """
                请为以下章节生成单章摘要。

                【章节标题】
                %s

                【章节正文】
                %s

                输出包含：本章摘要、关键事件、人物变化、地点变化、伏笔变化。
                """.formatted(title, content);
    }

    public String compressionPrompt(String sourceType, String content) {
        return """
                请把以下%s压缩成一条阶段记忆。

                【待压缩内容】
                %s

                要求：保留主线推进、人物状态变化、地点移动、设定限制和伏笔状态。
                """.formatted(sourceType, content);
    }

    public String globalMemoryPrompt(String oldGlobal, String newMemory) {
        return """
                请根据旧全局总摘要和新增阶段记忆，更新全局总摘要。

                【旧全局总摘要】
                %s

                【新增阶段记忆】
                %s

                要求：输出一版新的全局总摘要，避免无限变长。
                """.formatted(blankToDefault(oldGlobal, "暂无"), newMemory);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
