package com.jjxmas.ainovelstudio.ai;

import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Component;

@Component
public class MockNovelAiClient implements NovelAiClient {

    @Override
    public AiGenerateResult generate(AiGenerateCommand command) {
        return AiGenerateResult.builder()
                .success(true)
                .content(mockContent(command))
                .modelName("mock-novel-model")
                .usage(Map.of("fallback", true))
                .build();
    }

    @Override
    public Flux<String> stream(AiGenerateCommand command) {
        String content = mockContent(command);
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < content.length(); start += 24) {
            chunks.add(content.substring(start, Math.min(start + 24, content.length())));
        }
        return Flux.fromIterable(chunks);
    }

    private String mockContent(AiGenerateCommand command) {
        if (command.getTaskType() == AiTaskType.SETTING_BLUEPRINT) {
            return """
                    {"corePremise":"普通都市背后存在隐藏的超凡秩序，主角必须在守住日常生活的同时找到自己的位置。","mainConflict":"主角想保护身边人，但隐藏势力争夺资源的冲突不断逼近普通生活。","worldPremise":"表层社会遵循现实规则，超凡力量通过资源、传承和组织暗中运行。","immutableRules":["超凡力量必须付出代价","重要传承不能凭空复制","普通社会不会立刻接受超凡真相"],"entities":{"characters":[{"key":"char_main_01","name":"林默","role":"protagonist","purpose":"从普通生活进入隐藏秩序的主角"},{"key":"char_support_01","name":"苏晚","role":"supporting","purpose":"连接主角日常生活与隐藏世界"},{"key":"char_antagonist_01","name":"顾沉","role":"antagonist","purpose":"代表资源垄断势力的对手"}],"organizations":[{"key":"org_main_01","name":"夜巡司","purpose":"维护城市超凡秩序"},{"key":"org_enemy_01","name":"赤曜会","purpose":"垄断传承资源"},{"key":"org_neutral_01","name":"旧档案馆","purpose":"保存被隐藏的历史"}],"locations":[{"key":"loc_city_01","name":"临川市","purpose":"主角的日常生活舞台"},{"key":"loc_archive_01","name":"旧档案馆","purpose":"揭开历史真相的关键地点"},{"key":"loc_market_01","name":"黑曜集市","purpose":"超凡资源交易场所"}],"items":[{"key":"item_token_01","name":"残缺玉牌","purpose":"引出主线传承"},{"key":"item_register_01","name":"夜巡名册","purpose":"记录超凡势力与人员"},{"key":"item_medicine_01","name":"回息药","purpose":"体现力量代价与资源争夺"}],"events":[{"key":"event_anchor_01","name":"玉牌出现","purpose":"主角获得主线线索"},{"key":"event_anchor_02","name":"第一次追捕","purpose":"主角被迫接触隐藏势力"},{"key":"event_anchor_03","name":"档案馆开门","purpose":"揭露旧历史"},{"key":"event_anchor_04","name":"黑曜集市冲突","purpose":"主角与敌对势力正面交锋"},{"key":"event_anchor_05","name":"名册失窃","purpose":"推动势力秩序失衡"}]}}
                    """;
        }
        if (command.getTaskType() == AiTaskType.SETTING_DRAFT) {
            return """
                    {"overview":"普通都市与隐藏超凡秩序并存，主角因残缺玉牌被卷入资源与传承争夺。","rules":[{"key":"rule_01","name":"代价守恒","ruleType":"ability","description":"超凡力量必须交换现实中的代价。","triggerCondition":"使用超凡力量时","effectResult":"获得短暂能力并承受身体或记忆损耗","limitations":"不能无限使用","cost":"体力和记忆","exceptions":"古老传承可暂时减轻代价","visibilityLevel":"public","importance":10,"examples":"越强的力量代价越高"}],"characters":[{"key":"char_main_01","name":"林默","narrativeRole":"protagonist","identity":"普通维修师","publicIdentity":"普通维修师","personality":"谨慎、重情","motivation":"保护身边人","background":"在临川市独自生活","coreGoal":"查清玉牌来历","innerNeed":"学会主动选择而不是逃避","coreFlaw":"过度不信任他人","bottomLine":"不牺牲无辜者","skillsSummary":"观察和维修能力","secretNotes":"能感知玉牌中的异常","importance":10},{"key":"char_support_01","name":"苏晚","narrativeRole":"supporting","identity":"档案馆研究员","publicIdentity":"地方史研究员","personality":"冷静、敏锐","motivation":"找回失踪的家族记录","background":"长期研究城市旧档案","coreGoal":"打开旧档案馆禁区","innerNeed":"承认自己也需要同伴","coreFlaw":"习惯隐瞒信息","bottomLine":"不让档案落入赤曜会","skillsSummary":"历史检索和符号解读","secretNotes":"知道玉牌的一部分来源","importance":7},{"key":"char_antagonist_01","name":"顾沉","narrativeRole":"antagonist","identity":"赤曜会执行者","publicIdentity":"资源商人","personality":"克制、功利","motivation":"垄断传承资源","background":"从底层资源争夺中崛起","coreGoal":"夺回玉牌","innerNeed":"证明自己配得上权力","coreFlaw":"把人当作资源","bottomLine":"不允许组织失去控制","skillsSummary":"谈判和追踪","secretNotes":"惧怕旧档案馆记录的真相","importance":8}],"organizations":[{"key":"org_main_01","name":"夜巡司","organizationType":"faction","publicMission":"维护城市秩序","realGoal":"控制超凡事件扩散","controlledResources":"官方档案和许可","powerScope":"临川市","entryRules":"接受登记与考核"},{"key":"org_enemy_01","name":"赤曜会","organizationType":"faction","publicMission":"推动资源流通","realGoal":"垄断传承资源","controlledResources":"黑曜集市和药材","powerScope":"城市地下市场","entryRules":"以资源或功劳换取资格"},{"key":"org_neutral_01","name":"旧档案馆","organizationType":"institution","publicMission":"保存地方史","realGoal":"守护被隐藏的历史","controlledResources":"旧时代记录","powerScope":"档案馆及周边","entryRules":"需要研究员引荐"}],"locations":[{"key":"loc_city_01","name":"临川市","locationType":"city","description":"普通城市，夜间活动复杂","keyFeatures":"生活区、旧城区、地下通道","entryConditions":"普通人可进入","availableResources":"日常信息和交通","riskLevel":"medium","rules":"表层社会不能公开超凡事件"},{"key":"loc_archive_01","name":"旧档案馆","locationType":"archive","description":"保存旧时代记录的封闭建筑","keyFeatures":"禁区、地下库、旧门","entryConditions":"需要研究员引荐","availableResources":"历史记录和符号线索","riskLevel":"high","rules":"未经许可不能进入禁区"},{"key":"loc_market_01","name":"黑曜集市","locationType":"market","description":"隐藏在旧城区的超凡交易场所","keyFeatures":"药材摊位、黑市拍卖、临时盟约","entryConditions":"持有入场信物","availableResources":"药物、情报、装备","riskLevel":"high","rules":"交易必须遵守集市契约"}],"items":[{"key":"item_token_01","name":"残缺玉牌","itemType":"artifact","description":"刻有未知符号的残缺玉牌","usageRules":"在特定地点可唤醒线索","limitations":"每次使用都会消耗记忆","rarity":"rare","status":"available"},{"key":"item_register_01","name":"夜巡名册","itemType":"document","description":"记录超凡人员和事件的名册","usageRules":"可查询登记信息","limitations":"部分内容被封锁","rarity":"uncommon","status":"available"},{"key":"item_medicine_01","name":"回息药","itemType":"medicine","description":"短暂恢复力量的药物","usageRules":"战斗后服用","limitations":"连续服用会产生依赖","rarity":"uncommon","status":"available"}],"relations":[{"sourceKey":"char_main_01","targetKey":"org_main_01","sourceType":"character","targetType":"organization","relationType":"registered_with","note":"主角被迫登记"},{"sourceKey":"char_antagonist_01","targetKey":"org_enemy_01","sourceType":"character","targetType":"organization","relationType":"member_of","note":"执行者与组织关系"},{"sourceKey":"org_enemy_01","targetKey":"loc_market_01","sourceType":"organization","targetType":"location","relationType":"controls","note":"赤曜会控制集市"}],"events":[{"key":"event_anchor_01","name":"玉牌出现","eventType":"reveal","description":"林默在维修旧物时发现残缺玉牌","eventTimeText":"故事开始前","locationKey":"loc_city_01","importance":10},{"key":"event_anchor_02","name":"第一次追捕","eventType":"conflict","description":"赤曜会开始追踪玉牌","eventTimeText":"第一卷前期","locationKey":"loc_city_01","importance":9},{"key":"event_anchor_03","name":"档案馆开门","eventType":"reveal","description":"苏晚带林默进入旧档案馆","eventTimeText":"第一卷中期","locationKey":"loc_archive_01","importance":8},{"key":"event_anchor_04","name":"黑曜集市冲突","eventType":"conflict","description":"主角为获得线索与赤曜会发生冲突","eventTimeText":"第一卷中后期","locationKey":"loc_market_01","importance":8},{"key":"event_anchor_05","name":"名册失窃","eventType":"turning_point","description":"夜巡名册失窃，城市秩序开始失衡","eventTimeText":"第一卷末","locationKey":"loc_city_01","importance":9}],"states":[{"entityKey":"char_main_01","entityType":"character","stateType":"identity","oldValue":{},"newValue":{"value":"已接触隐藏秩序"}},{"entityKey":"item_token_01","entityType":"item","stateType":"ownership","oldValue":{},"newValue":{"owner":"char_main_01"}}]}
                    """;
        }
        if (command.getTaskType() == AiTaskType.OUTLINE_WORKFLOW_DRAFT) {
            return """
                    {"globalOutline":{"title":"全局大纲","content":"【主线目标】林默从一个只想保住普通生活的小人物，被残缺玉牌推入隐藏秩序。他的长期目标不是单纯变强，而是在普通社会、夜巡司规则和赤曜会资源垄断之间找到能保护身边人的位置。\\n\\n【长期矛盾】故事核心矛盾是自由选择与秩序控制。夜巡司代表有边界的秩序，赤曜会代表资源垄断和强者逻辑，旧档案馆则保存被压下去的历史真相。林默每向前一步，都要在安全、代价和真相之间取舍。\\n\\n【分卷节奏】第一卷以玉牌入局为核心，完成规则认知、势力初见和主角主动入局；第二卷扩展黑曜集市和资源体系，让主角面对更复杂的利益交换；第三卷转向旧史回声，揭开玉牌、名册和档案馆之间的历史关系，并把冲突从城市局部推向更大秩序。\\n\\n【主角成长】林默前期谨慎、防御、逃避风险，中期学会组建临时同盟，后期必须主动承担选择后果。他的成长重点不是获得无代价能力，而是理解每次力量使用都会改变自己与他人的关系。\\n\\n【伏笔回收】残缺玉牌、夜巡名册、旧档案馆禁区、黑曜集市契约和回息药依赖都会分阶段回收。前期只揭示现象，中期揭示规则，后期揭示规则背后的制定者与历史代价。\\n\\n【写作约束】每章必须有明确行动目标、现实阻碍、设定代价和章末牵引。避免只讲设定；设定必须通过追捕、交易、登记、失窃、救援等具体事件进入剧情。"},"volumes":[{"volumeNo":1,"title":"第一卷 玉牌入局","summary":"主角发现玉牌并接触夜巡司、赤曜会和旧档案馆。","goal":"建立主角目标、能力代价和第一组核心关系。","estimatedWordCount":120000},{"volumeNo":2,"title":"第二卷 黑曜集市","summary":"主角进入更复杂的资源交易和势力冲突。","goal":"扩大地图与资源体系，形成长期对手。","estimatedWordCount":160000},{"volumeNo":3,"title":"第三卷 旧史回声","summary":"旧档案馆记录揭开传承真相。","goal":"回收前期伏笔并打开更大的历史冲突。","estimatedWordCount":180000}],"arcs":[{"volumeNo":1,"arcNo":1,"title":"玉牌出现","summary":"林默发现玉牌并被赤曜会追踪。","goal":"让主角意识到普通生活已无法完全保住。","conflict":"主角想逃离异常，但对手不断逼近。","estimatedChapterCount":8}],"chapters":[{"chapterNo":1,"volumeNo":1,"arcNo":1,"title":"第1章 旧物里的裂纹","outline":"林默维修旧物时发现残缺玉牌，玉牌引发短暂异常。结尾赤曜会的人注意到线索。","scenePlan":["维修旧物","玉牌异常","陌生人追踪"]},{"chapterNo":2,"volumeNo":1,"arcNo":1,"title":"第2章 夜里的来客","outline":"林默试图确认异常来源，却被顾沉派来的人试探。结尾苏晚出现提醒他不要报警。","scenePlan":["调查异常","遭遇试探","苏晚提醒"]},{"chapterNo":3,"volumeNo":1,"arcNo":1,"title":"第3章 第一次代价","outline":"主角被迫使用玉牌能力脱身，同时付出记忆模糊的代价。结尾夜巡司名册出现他的名字。","scenePlan":["追捕升级","能力触发","名册出现"]},{"chapterNo":4,"volumeNo":1,"arcNo":1,"title":"第4章 档案馆的门","outline":"苏晚带林默进入旧档案馆，解释城市隐藏秩序。结尾禁区门锁自行打开。","scenePlan":["进入档案馆","解释规则","禁区开启"]},{"chapterNo":5,"volumeNo":1,"arcNo":1,"title":"第5章 登记","outline":"夜巡司要求林默登记，林默发现登记意味着被纳入控制。结尾赤曜会提出交易。","scenePlan":["夜巡司登记","规则压力","赤曜会交易"]},{"chapterNo":6,"volumeNo":1,"arcNo":1,"title":"第6章 黑曜邀请函","outline":"林默为了查清玉牌来源，决定进入黑曜集市。结尾邀请函上出现他的真实姓名。","scenePlan":["权衡选择","取得邀请函","姓名显现"]},{"chapterNo":7,"volumeNo":1,"arcNo":1,"title":"第7章 集市规矩","outline":"主角初入黑曜集市，见到资源交易和代价守恒规则。结尾顾沉现身。","scenePlan":["进入集市","观察交易","顾沉现身"]},{"chapterNo":8,"volumeNo":1,"arcNo":1,"title":"第8章 名册失窃","outline":"集市冲突中夜巡名册失窃，林默被怀疑牵涉其中。结尾他决定主动查清真相。","scenePlan":["集市冲突","名册失窃","主动入局"]}]}
                    """;
        }
        if (command.getTaskType() == AiTaskType.CHAPTER_OUTLINE_CONTINUATION) {
            return mockChapterOutlineContinuation(command);
        }
        if (command.getTaskType() == AiTaskType.CHAPTER_SUMMARY) {
            return """
                    本章摘要：主角围绕章节目标推进事件，遭遇新的阻碍，并在结尾留下继续阅读的线索。
                    关键事件：目标推进、冲突升级、线索保留。
                    人物变化：主角获得新的经验或压力。
                    地点变化：围绕当前章节场景推进。
                    伏笔变化：保留一个后续可回收的线索。
                    """;
        }
        if (command.getTaskType() == AiTaskType.CHAPTER_FACT_EXTRACTION) {
            return """
                    {
                      "events": [
                        {
                          "eventType": "conflict",
                          "name": "本章核心冲突推进",
                          "description": "主角围绕当前目标行动，并在过程中遭遇新的阻碍或代价。",
                          "locationText": "",
                          "eventTimeText": "",
                          "importance": 7
                        }
                      ],
                      "stateChanges": [
                        {
                          "entityType": "character",
                          "entityName": "主角",
                          "stateType": "pressure",
                          "oldValue": {"value": "原有压力状态"},
                          "newValue": {"value": "压力进一步上升"}
                        }
                      ],
                      "relationChanges": [],
                      "foreshadowChanges": [
                        {
                          "threadKey": "chapter_mock_hook",
                          "threadTitle": "本章留下的新线索",
                          "threadType": "foreshadow",
                          "changeType": "setup",
                          "setupText": "本章结尾留下了一个后续可回收的线索。",
                          "progressText": "",
                          "payoffHint": "后续章节可以围绕这条线索展开。"
                        }
                      ],
                      "unresolvedThreads": [
                        {
                          "threadKey": "chapter_mock_goal",
                          "threadTitle": "当前章节后的未解目标",
                          "threadType": "goal",
                          "description": "下一章仍需承接当前目标和新出现的危险。",
                          "urgency": "high",
                          "targetChapterNo": 0
                        }
                      ],
                      "issues": []
                    }
                    """;
        }
        if (command.getTaskType() == AiTaskType.MEMORY_COMPRESSION) {
            return "阶段摘要：近期章节围绕主线目标持续推进，人物状态、地点移动和伏笔线索已压缩记录。";
        }
        if (command.getTaskType() == AiTaskType.GLOBAL_MEMORY_UPDATE) {
            return "全局总摘要：作品主线继续推进，已完成内容围绕核心冲突逐步扩展，重要设定与人物变化保持记录。";
        }
        if (command.getTaskType() == AiTaskType.IDEA_GENERATION) {
            return """
                    标题：低门槛长篇成长方案
                    卖点：主角目标清晰，开局事件容易写，升级反馈稳定。
                    世界观：普通现实背后藏着更大的资源秩序，主角从熟悉生活进入超凡规则。
                    主线冲突：主角想守住原本生活，但新秩序不断压缩他的选择空间。
                    预估字数：150万-220万字。
                    风险提示：中后期需要持续设计阶段目标，避免只靠升级重复推进。
                    """;
        }
        if (command.getTaskType() == AiTaskType.REWRITE) {
            return command.getUserPrompt() + "\n\n【按修改意见重写后的 mock 内容】\n目标更明确，冲突更具体，后续扩写空间继续保留。";
        }
        return """
                主角带着明确目标进入本章场景，却很快遇到与设定规则相关的阻碍。
                他尝试用已有经验解决问题，但对手或环境给出新的压力，让冲突继续升级。
                章节结尾保留一个新的选择或线索，推动下一章继续阅读。
                """;
    }

    private String mockChapterOutlineContinuation(AiGenerateCommand command) {
        Map<?, ?> context = command.getContext() instanceof Map<?, ?> map ? map : Map.of();
        int startChapterNo = numberValue(context.get("startChapterNo"), 1);
        int count = numberValue(context.get("count"), 10);
        List<Map<String, Object>> chapters = new ArrayList<>();
        for (int chapterNo = startChapterNo; chapterNo < startChapterNo + count; chapterNo++) {
            chapters.add(Map.of(
                    "chapterNo", chapterNo,
                    "volumeNo", 1,
                    "arcNo", 1,
                    "title", "第" + chapterNo + "章 延续的线索",
                    "outline", "承接上一章的行动结果，主角遭遇新的阻碍并推进长期冲突，结尾留下下一章线索。",
                    "scenePlan", List.of("承接上一章", "冲突升级", "结尾钩子")));
        }
        return JsonUtils.toJson(Map.of(
                "newVolumes", List.of(),
                "newArcs", List.of(),
                "chapters", chapters));
    }

    private int numberValue(Object value, int defaultValue) {
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
}
