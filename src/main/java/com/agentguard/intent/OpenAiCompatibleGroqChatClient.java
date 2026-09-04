package com.agentguard.intent;

import com.agentguard.exception.IntentProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

@Component
class OpenAiCompatibleGroqChatClient implements GroqChatClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleGroqChatClient.class);
    private final RestClient client;
    private final String apiKey;
    private final String model;

    OpenAiCompatibleGroqChatClient(RestClient groqRestClient,
            @Value("${groq.api-key:}") String apiKey,
            @Value("${groq.model:openai/gpt-oss-120b}") String model) {
        this.client = groqRestClient;
        this.apiKey = apiKey;
        this.model = model;
        log.info("GroqChatClient configured with model={}", model);
    }

    @Override
    public String complete(String systemPrompt, String userRequest) {
        if (apiKey.isBlank()) throw new IntentProviderException("Intent provider is not configured");
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("category", "preferredBrand", "maxAmount", "requiresCategoryMatch"),
                "properties", Map.of(
                        "category", Map.of("type", "string"),
                        "preferredBrand", Map.of("type", List.of("string", "null")),
                        "maxAmount", Map.of("type", "number", "exclusiveMinimum", 0),
                        "requiresCategoryMatch", Map.of("type", "boolean")));
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userRequest)),
                "response_format", Map.of("type", "json_schema",
                        "json_schema", Map.of("name", "purchase_intent", "strict", true, "schema", schema)));
        try {
            Map<?, ?> response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.error("Groq API error: status={} body={}", res.getStatusCode(), errorBody);
                        throw new IntentProviderException("Intent provider returned error " + res.getStatusCode());
                    })
                    .body(Map.class);
            Object choices = response == null ? null : response.get("choices");
            if (!(choices instanceof List<?> list) || list.isEmpty()
                    || !(list.getFirst() instanceof Map<?, ?> choice)
                    || !(choice.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)) {
                throw new IntentProviderException("Intent provider returned an unusable response");
            }
            return content;
        } catch (IntentProviderException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("Groq HTTP error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IntentProviderException("Intent provider request failed", e);
        } catch (RestClientException e) {
            log.error("Groq request failed: {}", e.getMessage());
            throw new IntentProviderException("Intent provider request failed", e);
        }
    }
}
