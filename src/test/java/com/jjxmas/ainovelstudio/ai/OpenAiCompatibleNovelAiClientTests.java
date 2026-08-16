package com.jjxmas.ainovelstudio.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.mapper.ModelConfigMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class OpenAiCompatibleNovelAiClientTests {

    @Test
    void realModeFailsExplicitlyWhenNoModelConfigIsAvailable() {
        ModelConfigMapper mapper = mock(ModelConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OpenAiCompatibleNovelAiClient client = new OpenAiCompatibleNovelAiClient(
                mapper,
                mock(MockNovelAiClient.class),
                "real");

        assertThatThrownBy(() -> client.generate(AiGenerateCommand.builder().build()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("MODEL_CONFIG_UNAVAILABLE");
    }

    @Test
    void realModeStreamFailsExplicitlyWhenNoModelConfigIsAvailable() {
        ModelConfigMapper mapper = mock(ModelConfigMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OpenAiCompatibleNovelAiClient client = new OpenAiCompatibleNovelAiClient(
                mapper,
                mock(MockNovelAiClient.class),
                "real");

        assertThatThrownBy(() -> client.stream(AiGenerateCommand.builder().build()).collectList().block())
                .isInstanceOf(BusinessException.class)
                .hasMessage("MODEL_CONFIG_UNAVAILABLE");
    }

    @Test
    void mockModeUsesMockClientOnlyWhenExplicitlySelected() {
        ModelConfigMapper mapper = mock(ModelConfigMapper.class);
        MockNovelAiClient mockClient = mock(MockNovelAiClient.class);
        AiGenerateCommand command = AiGenerateCommand.builder().build();
        when(mockClient.generate(command)).thenReturn(AiGenerateResult.builder()
                .success(true)
                .content("mock content")
                .build());
        when(mockClient.stream(command)).thenReturn(Flux.just("mock content"));
        OpenAiCompatibleNovelAiClient client = new OpenAiCompatibleNovelAiClient(mapper, mockClient, "mock");

        org.assertj.core.api.Assertions.assertThat(client.generate(command).getContent()).isEqualTo("mock content");
        org.assertj.core.api.Assertions.assertThat(client.stream(command).collectList().block())
                .containsExactly("mock content");
        verify(mockClient).generate(command);
        verify(mockClient).stream(command);
    }
}
