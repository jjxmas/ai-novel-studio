package com.jjxmas.ainovelstudio.ai;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型调用结果占位，真实 token 用量和原始响应后续再接入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResult {

    private Boolean success;

    private String content;

    private String modelName;

    private Map<String, Object> usage;

}
