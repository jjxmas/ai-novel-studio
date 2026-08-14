package com.jjxmas.ainovelstudio.ai;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.prompts.ChapterGenerationPrompts;
import com.jjxmas.ainovelstudio.prompts.IdeaGenerationPrompts;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    public String systemPrompt(AiTaskType taskType) {
        return switch (taskType) {
            case IDEA_GENERATION -> IdeaGenerationPrompts.IDEA_GENERATION_SYSTEM;
            case IDEA_EVALUATION -> IdeaGenerationPrompts.IDEA_EVALUATION_SYSTEM;
            case SETTING_BLUEPRINT -> "你是长篇小说设定规划器。只输出符合要求的 JSON，不要输出 Markdown、解释或代码围栏。所有实体都必须有稳定的 key。";
            case SETTING_DRAFT -> "你是长篇小说设定建造器。根据已确认蓝图生成结构化设定草案。只输出符合要求的 JSON，不要输出 Markdown、解释或代码围栏。不得创造蓝图之外的核心实体。";
            case OUTLINE_WORKFLOW_DRAFT -> "你是长篇小说大纲规划器。根据已确认设定生成可写作的大纲草案。只输出 JSON，不要输出 Markdown、解释或代码围栏。";
            case CHAPTER_GENERATION -> ChapterGenerationPrompts.CHAPTER_GENERATION_SYSTEM;
            case REWRITE -> "你是长篇网文改写助手。请根据用户修改意见重写内容，保持原目标、连续性和关键设定一致。";
            case CHAPTER_FACT_EXTRACTION -> "你是小说章节事实抽取助手。请从已完成章节正文中提取结构化事实变化。只输出合法 JSON，不要输出 Markdown、解释或代码围栏；不得编造正文中不存在的事实。";
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

    public String chapterGenerationPrompt(ChapterContext context) {
        return """
                请生成一章适合连载网文的正文。这是长篇连续章节，不是独立短篇。

                [TASK]
                生成第 %s 章《%s》的正文。

                [GOAL]
                严格完成当前章节大纲中的目标、阻碍、推进结果，并自然留下章节钩子。

                [PROJECT_PROFILE]
                %s

                [IMMUTABLE_SETTING]
                %s

                [STORY_PLAN]
                %s

                [CHAPTER_PLAN]
                %s

                [CONTINUITY]
                %s

                [CURRENT_STATE]
                %s

                [ACTIVE_THREADS]
                %s

                [MEMORY_STACK]
                %s

                [CONSTRAINTS]
                %s

                [ACCEPTANCE]
                1. 只输出小说正文，不输出解释、提纲、分析或总结。
                2. 如果存在上一章，开头必须自然承接上一章最后的动作、对话、地点或状态。
                3. 不要无理由跳时间、跳地点、跳视角；如必须转场，先做简短过渡。
                4. 不得另起炉灶创造新的主线，不得覆盖既有设定代价。
                5. 优先用具体动作、对白和场景细节推进，不要写成摘要。
                """.formatted(
                safeChapterNo(context),
                safeChapterTitle(context),
                renderProjectProfile(context),
                renderImmutableSetting(context),
                renderStoryPlan(context),
                renderChapterPlan(context),
                renderContinuity(context),
                renderCurrentState(context),
                renderActiveThreads(context),
                renderMemoryStack(context),
                renderConstraints(context));
    }

    public String rewritePrompt(ChapterContext context, String content) {
        return """
                请根据当前章节上下文重写下面的章节正文。

                [TASK]
                重写第 %s 章《%s》的正文，保持主线目标、连续性和关键设定不变。

                [CHAPTER_PLAN]
                %s

                [CONTINUITY]
                %s

                [CURRENT_STATE]
                %s

                [ACTIVE_THREADS]
                %s

                [MEMORY_STACK]
                %s

                [CONSTRAINTS]
                %s

                [ORIGINAL_CONTENT]
                %s

                [ACCEPTANCE]
                1. 只输出重写后的正文。
                2. 保持章节目标、连续性和风格一致。
                3. 不得擅自修改已经确认的设定和上一章已发生事实。
                """.formatted(
                safeChapterNo(context),
                safeChapterTitle(context),
                renderChapterPlan(context),
                renderContinuity(context),
                renderCurrentState(context),
                renderActiveThreads(context),
                renderMemoryStack(context),
                renderConstraints(context),
                content);
    }

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

    public String chapterFactExtractionPrompt(String title, String content) {
        return """
                请从以下章节正文中提取结构化事实变化。只输出 JSON，不要输出 Markdown、解释或代码围栏。

                【章节标题】
                %s

                【章节正文】
                %s

                【输出 JSON 结构】
                {
                  "events": [
                    {
                      "eventType": "conflict",
                      "name": "事件名",
                      "description": "按正文描述事件发生了什么",
                      "locationText": "地点文本，没有则为空字符串",
                      "eventTimeText": "时间描述，没有则为空字符串",
                      "importance": 1
                    }
                  ],
                  "stateChanges": [
                    {
                      "entityType": "character",
                      "entityName": "实体名",
                      "stateType": "injury",
                      "oldValue": {"value": "变更前，没有则空对象"},
                      "newValue": {"value": "变更后"}
                    }
                  ],
                  "relationChanges": [
                    {
                      "sourceType": "character",
                      "sourceName": "源实体名",
                      "targetType": "organization",
                      "targetName": "目标实体名",
                      "relationType": "ally",
                      "changeType": "create",
                      "note": "关系变化说明"
                    }
                  ],
                  "foreshadowChanges": [
                    {
                      "threadKey": "stable_thread_key",
                      "threadTitle": "伏笔标题",
                      "threadType": "foreshadow",
                      "changeType": "setup",
                      "setupText": "本章埋下了什么",
                      "progressText": "本章推进了什么，没有则空字符串",
                      "payoffHint": "后续可回收方向，没有则空字符串"
                    }
                  ],
                  "unresolvedThreads": [
                    {
                      "threadKey": "stable_thread_key",
                      "threadTitle": "未解线程标题",
                      "threadType": "mystery",
                      "description": "下一章必须承接的问题、危险、承诺或目标",
                      "urgency": "high",
                      "targetChapterNo": 0
                    }
                  ],
                  "issues": ["对正文中不够确定、命名模糊或需要人工确认的点做简短说明"]
                }

                要求：
                1. 只提取正文中已经发生或已经明确提出的事实。
                2. 不要把推测当成事实；不确定时写入 issues。
                3. 没有的数组必须输出空数组，不要省略字段。
                4. `threadKey` 要稳定、短小、可复用，优先使用英文下划线风格。
                5. `changeType` 建议使用：`create` / `update` / `end` / `setup` / `advance` / `payoff`。
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

    private int safeChapterNo(ChapterContext context) {
        return context == null
                || context.getCurrentChapter() == null
                || context.getCurrentChapter().getChapterNo() == null
                ? 0
                : context.getCurrentChapter().getChapterNo();
    }

    private String safeChapterTitle(ChapterContext context) {
        return context == null || context.getCurrentChapter() == null
                ? ""
                : blankToDefault(context.getCurrentChapter().getTitle(), "");
    }

    private String renderProjectProfile(ChapterContext context) {
        if (context == null || context.getProjectProfile() == null) {
            return "项目画像为空。";
        }
        ChapterContext.ProjectProfile profile = context.getProjectProfile();
        return """
                标题：%s
                类型：%s
                平台：%s
                全书目标字数：%d-%d
                单章目标字数：%d
                风格偏好：%s
                """.formatted(
                blankToDefault(profile.getTitle(), "未命名作品"),
                blankToDefault(profile.getGenres(), "未提供"),
                blankToDefault(profile.getPlatformTarget(), "未提供"),
                profile.getTargetWordCountMin() == null ? 0 : profile.getTargetWordCountMin(),
                profile.getTargetWordCountMax() == null ? 0 : profile.getTargetWordCountMax(),
                profile.getTargetChapterWordCount() == null ? 3000 : profile.getTargetChapterWordCount(),
                blankToDefault(profile.getStylePreference(), "未提供"));
    }

    private String renderImmutableSetting(ChapterContext context) {
        if (context == null || context.getImmutableSetting() == null) {
            return "未提供设定总览。";
        }
        ChapterContext.ImmutableSetting setting = context.getImmutableSetting();
        return """
                设定摘要：%s
                设定总览：%s
                """.formatted(
                blankToDefault(setting.getSettingSummary(), "未提供"),
                blankToDefault(setting.getSettingOverview(), "未提供"));
    }

    private String renderStoryPlan(ChapterContext context) {
        if (context == null || context.getStoryPlan() == null) {
            return "未提供全局大纲。";
        }
        return blankToDefault(context.getStoryPlan().getGlobalOutline(), "未提供全局大纲。");
    }

    private String renderChapterPlan(ChapterContext context) {
        if (context == null || context.getCurrentChapter() == null) {
            return "未提供当前章节大纲。";
        }
        ChapterContext.CurrentChapter chapter = context.getCurrentChapter();
        return """
                标题：%s
                大纲：%s
                场景计划：%s
                """.formatted(
                blankToDefault(chapter.getTitle(), "未提供"),
                blankToDefault(chapter.getOutline(), "未提供"),
                renderList(chapter.getScenePlan()));
    }

    private String renderContinuity(ChapterContext context) {
        if (context == null || context.getContinuity() == null) {
            return "未提供连续性信息。";
        }
        ChapterContext.Continuity continuity = context.getContinuity();
        if (!Boolean.TRUE.equals(continuity.getHasPreviousChapter())) {
            return """
                    上一章：无
                    开场要求：%s
                    本章任务：%s
                    """.formatted(
                    blankToDefault(continuity.getOpeningRequirement(), "建立本章主场景。"),
                    blankToDefault(continuity.getChapterTask(), "完成当前章节目标。"));
        }
        return """
                上一章：第 %d 章《%s》
                上一章摘要：%s
                关键事件：%s
                人物变化：%s
                地点变化：%s
                伏笔变化：%s
                结尾片段：%s
                开场要求：%s
                必须承接：%s
                本章任务：%s
                """.formatted(
                continuity.getPreviousChapterNo() == null ? 0 : continuity.getPreviousChapterNo(),
                blankToDefault(continuity.getPreviousChapterTitle(), "未提供"),
                blankToDefault(continuity.getPreviousChapterSummary(), "未提供"),
                renderList(continuity.getPreviousKeyEvents()),
                renderList(continuity.getPreviousCharacterChanges()),
                renderList(continuity.getPreviousLocationChanges()),
                renderList(continuity.getPreviousForeshadowChanges()),
                blankToDefault(continuity.getPreviousChapterTail(), "未提供"),
                blankToDefault(continuity.getOpeningRequirement(), "自然承接上一章。"),
                renderList(continuity.getCarryForwardRequirements()),
                blankToDefault(continuity.getChapterTask(), "完成当前章节目标。"));
    }

    private String renderCurrentState(ChapterContext context) {
        if (context == null || context.getCurrentState() == null) {
            return "Current state snapshot is not available.";
        }
        ChapterContext.CurrentState currentState = context.getCurrentState();
        return """
                relevantCharacters: %s
                relevantOrganizations: %s
                relevantLocations: %s
                relevantItems: %s
                relevantRelations: %s
                relevantStateRecords: %s
                note: If a field is empty, do not invent hidden facts that are not present in the provided context.
                """.formatted(
                renderList(currentState.getRelevantCharacters()),
                renderList(currentState.getRelevantOrganizations()),
                renderList(currentState.getRelevantLocations()),
                renderList(currentState.getRelevantItems()),
                renderList(currentState.getRelevantRelations()),
                renderList(currentState.getRelevantStateRecords()));
    }

    private String renderActiveThreads(ChapterContext context) {
        if (context == null || context.getActiveThreads() == null) {
            return "暂无活动中的长期线程。";
        }
        ChapterContext.ActiveThreads activeThreads = context.getActiveThreads();
        return """
                未解线程：%s
                活跃伏笔：%s
                """.formatted(
                renderList(activeThreads.getUnresolvedThreads()),
                renderList(activeThreads.getActiveForeshadowThreads()));
    }

    private String renderMemoryStack(ChapterContext context) {
        if (context == null || context.getMemoryStack() == null) {
            return "暂无记忆层信息。";
        }
        ChapterContext.MemoryStack memoryStack = context.getMemoryStack();
        return """
                全局摘要：%s
                高层摘要：%s
                中层摘要：%s
                近窗摘要：%s
                """.formatted(
                blankToDefault(memoryStack.getGlobalMemory(), "暂无"),
                renderList(memoryStack.getHighMemories()),
                renderList(memoryStack.getMiddleMemories()),
                renderList(memoryStack.getRecentSummaries()));
    }

    private String renderConstraints(ChapterContext context) {
        if (context == null || context.getGenerationConstraints() == null) {
            return "无额外约束。";
        }
        ChapterContext.GenerationConstraints constraints = context.getGenerationConstraints();
        return """
                单章目标字数：%d
                风格偏好：%s
                用户要求：%s
                数据质量警告：%s
                """.formatted(
                constraints.getTargetChapterWordCount() == null ? 3000 : constraints.getTargetChapterWordCount(),
                blankToDefault(constraints.getStylePreference(), "未提供"),
                blankToDefault(constraints.getUserAdvice(), "无"),
                renderList(constraints.getDataQualityWarnings()));
    }

    private String renderList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        StringJoiner joiner = new StringJoiner("；");
        for (String value : values) {
            String item = blankToDefault(value, "");
            if (!item.isBlank()) {
                joiner.add(item);
            }
        }
        return joiner.length() == 0 ? "无" : joiner.toString();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
