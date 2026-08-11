package com.jjxmas.ainovelstudio.ai;

import java.util.Map;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.prompts.ChapterGenerationPrompts;
import com.jjxmas.ainovelstudio.prompts.IdeaGenerationPrompts;
import org.springframework.stereotype.Service;

/**
 * 提示词模板服务，集中生成不同 AI 任务的系统提示词和用户提示词。
 */
@Service
public class PromptTemplateService {

    /**
     * 根据任务类型返回对应的系统提示词。
     */
    public String systemPrompt(AiTaskType taskType) {
        return switch (taskType) {
            case IDEA_GENERATION -> IdeaGenerationPrompts.IDEA_GENERATION_SYSTEM;
            case IDEA_EVALUATION -> IdeaGenerationPrompts.IDEA_EVALUATION_SYSTEM;
            case SETTING_BLUEPRINT -> "你是长篇小说设定规划器。只输出符合要求的 JSON，不要输出 Markdown、解释或代码围栏。所有实体都必须有稳定的 key。";
            case SETTING_DRAFT -> "你是长篇小说设定建造器。根据已确认蓝图生成结构化设定草案。只输出符合要求的 JSON，不要输出 Markdown、解释或代码围栏。不得创造蓝图之外的核心实体。";
            case OUTLINE_WORKFLOW_DRAFT -> "你是长篇小说大纲规划器。根据已确认设定生成可写作的大纲草案。只输出 JSON，不要输出 Markdown、解释或代码围栏。";
            case CHAPTER_GENERATION -> ChapterGenerationPrompts.CHAPTER_GENERATION_SYSTEM;
            case REWRITE -> "你是长篇网文改写助手。请根据用户修改意见重写内容，保持原目标和关键设定一致。";
            case CHAPTER_SUMMARY -> "你是小说章节摘要助手。请提取剧情、人物状态、地点移动和伏笔变化，输出中文摘要。";
            case MEMORY_COMPRESSION -> "你是长篇小说记忆压缩助手。请把多条摘要压缩成一条中高层记忆，保留主线、人物变化和伏笔。";
            case GLOBAL_MEMORY_UPDATE -> "你是长篇小说总摘要维护助手。请根据旧总摘要和新阶段摘要更新全局总摘要。";
            default -> "你是小说创作助手。请按用户要求输出中文内容。";
        };
    }

    public String settingBlueprintPrompt(Map<String, Object> context) {
        return """
                根据下面的作品和已选创意生成设定蓝图。
                【输入】
                %s

                【输出 JSON 结构】
                {
                  "corePremise": "作品核心前提",
                  "mainConflict": "主线冲突",
                  "worldPremise": "世界前提",
                  "immutableRules": ["不可改写的硬规则"],
                  "entities": {
                    "characters": [{"key":"char_main_01","name":"","role":"protagonist","purpose":""}],
                    "organizations": [{"key":"org_main_01","name":"","purpose":""}],
                    "locations": [{"key":"loc_main_01","name":"","purpose":""}],
                    "items": [{"key":"item_main_01","name":"","purpose":""}],
                    "events": [{"key":"event_anchor_01","name":"","purpose":""}]
                  }
                }

                要求：角色 3-5 个，组织 3-5 个，地点 3-5 个，物品 3-8 个，事件 5-10 个；key 使用英文、数字和下划线。
                """.formatted(context);
    }

    public String settingDraftPrompt(Map<String, Object> context, Object blueprint) {
        return """
                根据已确认的设定蓝图生成结构化设定草案。
                【作品上下文】
                %s

                【已确认蓝图】
                %s

                【输出 JSON 结构】
                {
                  "overview":"设定总览",
                  "rules":[{"key":"rule_01","name":"","ruleType":"general","description":"","triggerCondition":"","effectResult":"","limitations":"","cost":"","exceptions":"","visibilityLevel":"public","importance":1,"examples":""}],
                  "characters":[{"key":"char_main_01","name":"","narrativeRole":"protagonist","identity":"","publicIdentity":"","personality":"","motivation":"","background":"","coreGoal":"","innerNeed":"","coreFlaw":"","bottomLine":"","skillsSummary":"","secretNotes":"","importance":10}],
                  "organizations":[{"key":"org_main_01","name":"","organizationType":"faction","publicMission":"","realGoal":"","controlledResources":"","powerScope":"","entryRules":""}],
                  "locations":[{"key":"loc_main_01","name":"","locationType":"place","description":"","keyFeatures":"","entryConditions":"","availableResources":"","riskLevel":"medium","rules":""}],
                  "items":[{"key":"item_main_01","name":"","itemType":"item","description":"","usageRules":"","limitations":"","rarity":"","status":"available"}],
                  "relations":[{"sourceKey":"char_main_01","targetKey":"org_main_01","sourceType":"character","targetType":"organization","relationType":"knows","note":""}],
                  "events":[{"key":"event_anchor_01","name":"","eventType":"story","description":"","eventTimeText":"","locationKey":"loc_main_01","importance":1}],
                  "states":[{"entityKey":"char_main_01","entityType":"character","stateType":"identity","oldValue":{},"newValue":{"value":""}}]
                }

                要求：只使用蓝图中的 key；所有数组必须存在；关系、事件地点和状态实体不得引用不存在的 key。
                """.formatted(context, blueprint);
    }

    public String outlineWorkflowDraftPrompt(Map<String, Object> context) {
        return """
                根据已确认设定库生成大纲工作流草案。
                【输入】
                %s

                【输出 JSON 结构】
                {
                  "globalOutline":{"title":"全局大纲","content":"至少 800 字，分段写清主线目标、长期矛盾、分卷节奏、主角成长、关键伏笔和写作约束"},
                  "volumes":[{"volumeNo":1,"title":"第一卷","summary":"","goal":"","estimatedWordCount":120000}],
                  "arcs":[{"volumeNo":1,"arcNo":1,"title":"","summary":"","goal":"","conflict":"","estimatedChapterCount":10}],
                  "chapters":[{"chapterNo":1,"volumeNo":1,"arcNo":1,"title":"","outline":"","scenePlan":["开场目标","冲突升级","结尾钩子"]}]
                }

                要求：
                1. globalOutline.content 必须是完整全局大纲，不是摘要；至少包含【主线目标】【长期矛盾】【分卷节奏】【主角成长】【伏笔回收】【写作约束】六段。
                2. 生成 3-5 个分卷，每卷都要有独立目标、转折和结局牵引。
                3. 第一版只展开第一卷剧情单元。
                4. 只生成第一批 5-10 章章节大纲。
                5. 每章必须有目标、阻碍、推进结果和结尾钩子。
                6. 不得违背已确认设定库。
                """.formatted(context);
    }




    /**
     * 生成创意重写任务的用户提示词。
     */
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

    /**
     * 生成章节正文生成任务的用户提示词。
     */
    public String chapterGenerationPrompt(Map<String, Object> context, String title, String outline, String advice) {
        return """
                请生成一章适合连载网文的正文。这是长篇连续章节，不是独立短篇。

                【结构化上下文 JSON】
                %s

                【章节标题】
                %s

                【章节大纲】
                %s

                【用户要求】
                %s

                要求：
                1. 不要写解释，不要输出大纲。
                2. 如果“上一章连续性.存在上一章”为 true，开头必须直接承接“上一章连续性.结尾片段”的最后动作、对白、地点和人物状态。
                3. 不要无理由跳时间、换地点、换视角；如必须跳转，先用一两句话交代过渡。
                4. 必须处理上一章连续性里的未解决事项、关键事件、人物变化、地点变化和伏笔变化。
                5. 严格执行“本章承接契约”和当前章节大纲，避免另起炉灶创造新主线。
                6. 尽量减少机械总结感，多用具体动作、对话和场景细节。
                7. 结尾保留自然钩子，但不要覆盖已经建立的人物状态和设定代价。
                """.formatted(JsonUtils.toJson(context), title, outline, blankToDefault(advice, "无"));
    }

    /**
     * 生成章节正文重写任务的用户提示词。
     */
    public String rewritePrompt(Map<String, Object> context, String content, String instruction) {
        return """
                请根据修改意见重写下面的章节正文。

                【结构化上下文 JSON】
                %s

                【原正文】
                %s

                【修改意见】
                %s

                要求：只输出重写后的正文，保持连续性和风格一致。
                """.formatted(JsonUtils.toJson(context), content, instruction);
    }

    /**
     * 生成章节摘要任务的用户提示词。
     */
    public String chapterSummaryPrompt(String title, String content) {
        return """
                请为以下章节生成单章结构化摘要。只输出 JSON，不要输出 Markdown、解释或代码围栏。

                【章节标题】
                %s

                【章节正文】
                %s

                【输出 JSON 结构】
                {
                  "summary": "本章 200-400 字摘要",
                  "keyEvents": ["关键事件，按发生顺序"],
                  "characterChanges": ["人物状态、立场、伤势、关系或心理变化"],
                  "locationChanges": ["时间地点移动和场景状态变化"],
                  "foreshadowChanges": ["新增、推进或回收的伏笔"],
                  "endingState": "章末最后一幕的地点、在场人物、动作和情绪状态",
                  "unresolvedThreads": ["下一章必须承接的未解决目标、危险、承诺或线索"]
                }
                """.formatted(title, content);
    }

    /**
     * 生成记忆压缩任务的用户提示词。
     */
    public String compressionPrompt(String sourceType, String content) {
        return """
                请把以下%s压缩成一条阶段记忆。

                【待压缩内容】
                %s

                要求：保留主线推进、人物状态变化、地点移动、设定限制和伏笔状态。
                """.formatted(sourceType, content);
    }

    /**
     * 生成全局记忆更新任务的用户提示词。
     */
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

    /**
     * 为空白文本提供默认值。
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
