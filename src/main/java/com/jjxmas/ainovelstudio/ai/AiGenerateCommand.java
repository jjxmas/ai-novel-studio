package com.jjxmas.ainovelstudio.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型调用命令。Spring AI ChatClient 适配器统一读取这个对象。
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

    private Object context;

    private Integer maxTokens;

    private Double temperature;
}
