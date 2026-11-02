package com.claircore.assistant.infrastructure.llm.deepseek;

import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelReply;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek chat completions over its OpenAI-compatible API.
 *
 * <p>Reasoning is switched off: with it on, {@code deepseek-flash} can spend the whole token budget
 * thinking and return an empty answer. Neither the API key nor the prompt is ever logged; only
 * status, latency and token counts are.
 */
@Service
public class DeepSeekLanguageModelClient implements LanguageModelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekLanguageModelClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public DeepSeekLanguageModelClient(
            @Value("${deepseek.api-url}") String apiUrl,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.model}") String model,
            @Value("${deepseek.max-tokens}") int maxTokens,
            @Value("${deepseek.temperature}") double temperature
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restTemplate = new RestTemplate(requestFactory);
        this.apiUrl = stripTrailingSlash(apiUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @CircuitBreaker(name = "deepseek", fallbackMethod = "completeFallback")
    public LanguageModelReply complete(List<LanguageModelMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssistantUnavailableException("Assistant is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages.stream()
                .map(message -> Map.of("role", message.role(), "content", message.content()))
                .toList());
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("thinking", Map.of("type", "disabled"));
        body.put("stream", false);

        long startedAt = System.nanoTime();
        ResponseEntity<ChatCompletionResponse> response;
        try {
            response = restTemplate.postForEntity(
                    apiUrl + "/chat/completions", new HttpEntity<>(body, headers), ChatCompletionResponse.class);
        } catch (RestClientException e) {
            LOGGER.warn("DeepSeek call failed after {} ms: {}", elapsedMillis(startedAt), e.getClass().getSimpleName());
            throw new AssistantUnavailableException("Assistant is temporarily unavailable", e);
        }
        int latencyMs = elapsedMillis(startedAt);

        ChatCompletionResponse completion = response.getBody();
        String content = completion == null ? null : completion.firstContent();
        if (!response.getStatusCode().is2xxSuccessful() || content == null || content.isBlank()) {
            LOGGER.warn("DeepSeek returned no usable answer: status={} finishReason={} latencyMs={}",
                    response.getStatusCode().value(), completion == null ? null : completion.firstFinishReason(), latencyMs);
            throw new AssistantUnavailableException("Assistant returned an empty answer");
        }

        Usage usage = completion.usage();
        Integer promptTokens = usage == null ? null : usage.promptTokens();
        Integer completionTokens = usage == null ? null : usage.completionTokens();
        LOGGER.info("DeepSeek answered: latencyMs={} promptTokens={} completionTokens={}",
                latencyMs, promptTokens, completionTokens);
        return new LanguageModelReply(
                content.strip(),
                completion.model() == null ? model : completion.model(),
                promptTokens,
                completionTokens,
                latencyMs);
    }

    private LanguageModelReply completeFallback(List<LanguageModelMessage> messages, Throwable ex) {
        if (ex instanceof AssistantUnavailableException unavailable) {
            throw unavailable;
        }
        LOGGER.warn("Circuit breaker active for DeepSeek: {}", ex.getClass().getSimpleName());
        throw new AssistantUnavailableException("Assistant is temporarily unavailable", ex);
    }

    private static int elapsedMillis(long startedAtNanos) {
        return (int) Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(String model, List<Choice> choices, Usage usage) {
        String firstContent() {
            if (choices == null || choices.isEmpty() || choices.getFirst().message() == null) return null;
            return choices.getFirst().message().content();
        }

        String firstFinishReason() {
            return choices == null || choices.isEmpty() ? null : choices.getFirst().finishReason();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                 @JsonProperty("completion_tokens") Integer completionTokens) {
    }
}
