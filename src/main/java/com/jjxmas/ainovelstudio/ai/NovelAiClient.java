package com.jjxmas.ainovelstudio.ai;

import reactor.core.publisher.Flux;

public interface NovelAiClient {

    AiGenerateResult generate(AiGenerateCommand command);

    Flux<String> stream(AiGenerateCommand command);
}
