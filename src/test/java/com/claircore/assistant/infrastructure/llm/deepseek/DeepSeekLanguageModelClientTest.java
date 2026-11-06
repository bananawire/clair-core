package com.claircore.assistant.infrastructure.llm.deepseek;

import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekLanguageModelClientTest {

    private static final List<LanguageModelMessage> PROMPT = List.of(
            LanguageModelMessage.system("rules"),
            LanguageModelMessage.user("Is my air OK?"));

    @Test
    void sendsAChatCompletionWithReasoningDisabledAndReadsTheAnswer() {
        var client = client("test-key");
        var server = bind(client);
        server.expect(requestTo("https://api.deepseek.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("deepseek-flash"))
                .andExpect(jsonPath("$.max_tokens").value(600))
                .andExpect(jsonPath("$.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Is my air OK?"))
                .andRespond(withSuccess("""
                        {"model":"deepseek-flash","choices":[{"index":0,"finish_reason":"stop",
                          "message":{"role":"assistant","content":"  Yes, CO2 is fine.  "}}],
                         "usage":{"prompt_tokens":120,"completion_tokens":9,"total_tokens":129}}
                        """, MediaType.APPLICATION_JSON));

        var reply = client.complete(PROMPT);

        server.verify();
        assertThat(reply.content()).isEqualTo("Yes, CO2 is fine.");
        assertThat(reply.model()).isEqualTo("deepseek-flash");
        assertThat(reply.promptTokens()).isEqualTo(120);
        assertThat(reply.completionTokens()).isEqualTo(9);
        assertThat(reply.latencyMs()).isNotNegative();
    }

    @Test
    void isUnavailableWithoutCallingTheApiWhenNoKeyIsConfigured() {
        var client = client("");
        var server = bind(client);

        assertThatThrownBy(() -> client.complete(PROMPT))
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessage("Assistant is not configured");
        server.verify();
    }

    @Test
    void anEmptyAnswerCountsAsUnavailable() {
        var client = client("test-key");
        var server = bind(client);
        server.expect(requestTo("https://api.deepseek.test/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"finish_reason":"length","message":{"role":"assistant","content":""}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(PROMPT)).isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void aServerErrorCountsAsUnavailable() {
        var client = client("test-key");
        var server = bind(client);
        server.expect(requestTo("https://api.deepseek.test/chat/completions")).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(PROMPT))
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessage("Assistant is temporarily unavailable");
    }

    private static DeepSeekLanguageModelClient client(String apiKey) {
        return new DeepSeekLanguageModelClient("https://api.deepseek.test/", apiKey, "deepseek-flash", 600, 0.3);
    }

    private static MockRestServiceServer bind(DeepSeekLanguageModelClient client) {
        var restTemplate = new RestTemplate();
        client.setRestTemplate(restTemplate);
        return MockRestServiceServer.bindTo(restTemplate).build();
    }
}
