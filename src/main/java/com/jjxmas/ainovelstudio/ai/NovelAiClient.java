package com.jjxmas.ainovelstudio.ai;

/**
 * AI 调用端口。不允许业务模块直接调用外部模型 API。
 */
public interface NovelAiClient {

    /**
     * 执行一次小说创作相关的 AI 生成命令。
     */
    AiGenerateResult generate(AiGenerateCommand command);
}
