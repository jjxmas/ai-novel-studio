package com.jjxmas.ainovelstudio.ai;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 模型兜底实现。真实模型不可用时仍保证创作流程可继续。
 */
@Component
public class MockNovelAiClient implements NovelAiClient {

    @Override
    public AiGenerateResult generate(AiGenerateCommand command) {
        return AiGenerateResult.builder()
                .success(true)
                .content(mockContent(command))
                .modelName("mock-novel-model")
                .usage(Map.of("fallback", true))
                .rawResponse("")
                .build();
    }

    private String mockContent(AiGenerateCommand command) {
        if (command.getTaskType() == AiTaskType.CHAPTER_SUMMARY) {
            return """
                    本章摘要：主角围绕章节目标推进事件，遭遇新的阻碍，并在结尾留下继续阅读的线索。
                    关键事件：目标推进、冲突升级、线索保留。
                    人物变化：主角获得新的经验或压力。
                    地点变化：围绕当前章节场景推进。
                    伏笔变化：保留一个后续可回收的线索。
                    """;
        }
        if (command.getTaskType() == AiTaskType.MEMORY_COMPRESSION) {
            return "阶段摘要：近期章节围绕主线目标持续推进，人物状态、地点移动和伏笔线索已压缩记录。";
        }
        if (command.getTaskType() == AiTaskType.GLOBAL_MEMORY_UPDATE) {
            return "全局总摘要：作品主线继续推进，已完成内容围绕核心冲突逐步扩展，重要设定与人物变化保持记录。";
        }
        if (command.getTaskType() == AiTaskType.REWRITE) {
            return command.getUserPrompt() + "\n\n【按修改意见重写后的 mock 正文】\n人物目标更明确，场景动作更具体，结尾继续保留章节钩子。";
        }
        return """
                主角带着明确目标进入本章场景，却很快遇到与设定规则相关的阻碍。
                他尝试用已有经验解决问题，但对手或环境给出新的压力，让冲突继续升级。
                章节结尾保留一个新的选择或线索，推动下一章继续阅读。
                """;
    }
}
