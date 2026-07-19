package com.jjxmas.ainovelstudio.ai;

import com.jjxmas.ainovelstudio.module.model.entity.ModelConfig;
import com.jjxmas.ainovelstudio.module.model.mapper.ModelConfigMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * OpenAI-compatible 调用适配器。后续可替换为 Spring AI ChatClient 实现。
 */
@Primary
@Component
public class OpenAiCompatibleNovelAiClient implements NovelAiClient {

    private final ModelConfigMapper modelConfigMapper;
    private final MockNovelAiClient mockNovelAiClient;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleNovelAiClient(
            ModelConfigMapper modelConfigMapper,
            MockNovelAiClient mockNovelAiClient,
            RestTemplateBuilder restTemplateBuilder) {
        this.modelConfigMapper = modelConfigMapper;
        this.mockNovelAiClient = mockNovelAiClient;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public AiGenerateResult generate(AiGenerateCommand command) {
        ModelConfig config = resolveConfig(command.getModelConfigId());
        if (config == null || config.getApiKeyCiphertext() == null || config.getApiKeyCiphertext().isBlank()) {
            return mockNovelAiClient.generate(command);
        }
        try {
            Map<?, ?> response = restTemplate.postForObject(
                    chatCompletionsUrl(config.getBaseUrl()),
                    new HttpEntity<>(requestBody(command, config), headers(config)),
                    Map.class);
            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                return mockNovelAiClient.generate(command);
            }
            return AiGenerateResult.builder()
                    .success(true)
                    .content(content)
                    .modelName(config.getModelName())
                    .usage(rawUsage(response))
                    .rawResponse(String.valueOf(response))
                    .build();
        } catch (RestClientException | IllegalArgumentException ex) {
            return mockNovelAiClient.generate(command);
        }
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

    private Map<String, Object> requestBody(AiGenerateCommand command, ModelConfig config) {
        return Map.of(
                "model", blankToDefault(config.getModelName(), "gpt-4o-mini"),
                "temperature", command.getTemperature() == null ? 0.7 : command.getTemperature(),
                "messages", List.of(
                        Map.of("role", "system", "content", blankToEmpty(command.getSystemPrompt())),
                        Map.of("role", "user", "content", blankToEmpty(command.getUserPrompt()))));
    }

    private HttpHeaders headers(ModelConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(decodeApiKey(config.getApiKeyCiphertext()));
        return headers;
    }

    private String chatCompletionsUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com/v1"
                : baseUrl.replaceAll("/+$", "");
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        return normalized + "/chat/completions";
    }

    private String decodeApiKey(String ciphertext) {
        return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Map<String, Object> rawUsage(Map<?, ?> response) {
        if (response == null || response.get("usage") == null) {
            return Map.of();
        }
        return Map.of("rawUsage", response.get("usage"));
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return null;
        }
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        return content == null ? null : String.valueOf(content);
    }
}
