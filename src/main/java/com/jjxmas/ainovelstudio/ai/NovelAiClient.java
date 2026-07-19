package com.jjxmas.ainovelstudio.ai;

/**
 * AI 调用端口。后续由 Spring AI 实现，不允许业务模块直接调用外部模型 API。
 */
public interface NovelAiClient {

    AiGenerateResult generate(AiGenerateCommand command);
}
