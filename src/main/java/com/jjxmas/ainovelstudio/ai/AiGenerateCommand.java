package com.jjxmas.ainovelstudio.ai;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型调用命令。后续 Spring AI 适配器统一读取这个对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateCommand {

    private Long modelConfigId;

    private AiTaskType taskType;

    private String systemPrompt;

    private String userPrompt;

    private Map<String, Object> context;

    private Integer maxTokens;

    private Double temperature;
}
