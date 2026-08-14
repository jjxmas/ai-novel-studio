package com.jjxmas.ainovelstudio.ai;

import com.jjxmas.ainovelstudio.pojo.entity.ModelConfig;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import java.util.Map;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Primary
@Component
public class OpenAiCompatibleNovelAiClient implements NovelAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleNovelAiClient.class);

    private final ModelConfigMapper modelConfigMapper;
    private final MockNovelAiClient mockNovelAiClient;

    public OpenAiCompatibleNovelAiClient(
            ModelConfigMapper modelConfigMapper,
            MockNovelAiClient mockNovelAiClient) {
        this.modelConfigMapper = modelConfigMapper;
        this.mockNovelAiClient = mockNovelAiClient;
    }

    @Override
    public AiGenerateResult generate(AiGenerateCommand command) {
        ModelConfig config = resolveConfig(command.getModelConfigId());
        if (config == null || config.getApiKeyCiphertext() == null || config.getApiKeyCiphertext().isBlank()) {
            return null;
        }
        try {
            String content = chatClient(command, config)
                    .prompt()
                    .system(blankToEmpty(command.getSystemPrompt()))
                    .user(blankToEmpty(command.getUserPrompt()))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return null;
            }
            return AiGenerateResult.builder()
                    .success(true)
                    .content(content)
                    .modelName(config.getModelName())
                    .usage(Map.of("springAiChatClient", true))
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Spring AI ChatClient 调用失败，已回退 mock。modelConfigId={}, modelName={}",
                    config.getId(),
                    config.getModelName(),
                    ex);
            return null;
        }
    }

    @Override
    public Flux<String> stream(AiGenerateCommand command) {
        return Flux.defer(() -> {
            ModelConfig config = resolveConfig(command.getModelConfigId());
            if (config == null || config.getApiKeyCiphertext() == null || config.getApiKeyCiphertext().isBlank()) {
                return mockNovelAiClient.stream(command);
            }
            try {
                return chatClient(command, config)
                        .prompt()
                        .system(blankToEmpty(command.getSystemPrompt()))
                        .user(blankToEmpty(command.getUserPrompt()))
                        .stream()
                        .content()
                        .filter(content -> content != null && !content.isEmpty())
                        .onErrorResume(ex -> {
                            log.warn("Spring AI streaming call failed, fallback to mock. modelConfigId={}, modelName={}",
                                    config.getId(),
                                    config.getModelName(),
                                    ex);
                            return mockNovelAiClient.stream(command);
                        });
            } catch (RuntimeException ex) {
                log.warn("Spring AI streaming client creation failed, fallback to mock. modelConfigId={}, modelName={}",
                        config.getId(),
                        config.getModelName(),
                        ex);
                return mockNovelAiClient.stream(command);
            }
        });
    }

    private ModelConfig resolveConfig(Long modelConfigId) {
        if (modelConfigId != null) {
            ModelConfig config = modelConfigMapper.selectById(modelConfigId);
            if (config != null && !Boolean.FALSE.equals(config.getEnabled())) {
                return config;
            }
        }
        return modelConfigMapper.selectList(null)
                .stream()
                .filter((item) -> !Boolean.FALSE.equals(item.getEnabled()))
                .filter((item) -> Boolean.TRUE.equals(item.getDefaultModel()))
                .findFirst()
                .orElse(null);
    }

    private ChatClient chatClient(AiGenerateCommand command, ModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(config.getBaseUrl()))
                .apiKey(normalizeApiKey(config.getApiKeyCiphertext()))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelNameOrDefault(config.getModelName()))
                .temperature(command.getTemperature() == null ? 0.75 : command.getTemperature())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
        return ChatClient.create(chatModel);
    }

    private String normalizeApiKey(String apiKey) {
        String normalized = apiKey.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            normalized = normalized.substring("Bearer ".length()).trim();
        }
        return normalized;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String modelNameOrDefault(String value) {
        return value == null || value.isBlank() ? "gpt-5.4" : value;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com";
        }
        String normalized = baseUrl.replaceAll("/+$", "");
        if (normalized.endsWith("/v1/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/v1/chat/completions".length());
        }
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - "/v1".length());
        }
        return normalized;
    }
}
