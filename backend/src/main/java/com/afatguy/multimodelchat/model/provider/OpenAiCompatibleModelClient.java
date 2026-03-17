package com.afatguy.multimodelchat.model.provider;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiCompatibleModelClient implements ModelClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleModelClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean supports(String providerType) {
        return "OPENAI_COMPATIBLE".equalsIgnoreCase(providerType) || "CUSTOM_HTTP".equalsIgnoreCase(providerType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(ModelConfigEntity modelConfig, ChatCompletionRequest request) {
        Map<String, Object> body = buildBaseBody(modelConfig, request);

        Map<String, Object> response = restClient.post()
            .uri(modelConfig.getEndpoint())
            .contentType(MediaType.APPLICATION_JSON)
            .headers(headers -> applyHeaders(headers, modelConfig, false))
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return "";
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        Object content = message == null ? null : message.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    @Override
    public void streamChat(ModelConfigEntity modelConfig, ChatCompletionRequest request, Consumer<String> onDelta) {
        try {
            Map<String, Object> body = buildBaseBody(modelConfig, request);
            body.put("stream", true);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(modelConfig.getEndpoint()))
                .timeout(Duration.ofMillis(Math.max(1000, modelConfig.getTimeoutMs())))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

            if (modelConfig.getApiKeyEncrypted() != null && !modelConfig.getApiKeyEncrypted().isBlank()) {
                requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + modelConfig.getApiKeyEncrypted());
            }

            HttpResponse<java.io.InputStream> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("upstream status=" + response.statusCode() + " body=" + errBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    String delta = extractDelta(data);
                    if (!delta.isEmpty()) {
                        onDelta.accept(delta);
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("openai stream parse failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildBaseBody(ModelConfigEntity modelConfig, ChatCompletionRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelConfig.getModelCode());
        body.put("messages", request.messages().stream().map(msg -> Map.of("role", msg.role(), "content", msg.content())).toList());
        body.put("temperature", request.temperature() == null ? 0.7 : request.temperature());
        body.put("top_p", request.topP() == null ? 0.9 : request.topP());
        body.put("max_tokens", request.maxTokens() == null ? 1024 : request.maxTokens());
        return body;
    }

    private void applyHeaders(HttpHeaders headers, ModelConfigEntity modelConfig, boolean stream) {
        headers.set(HttpHeaders.ACCEPT, stream ? MediaType.TEXT_EVENT_STREAM_VALUE : MediaType.APPLICATION_JSON_VALUE);
        if (modelConfig.getApiKeyEncrypted() != null && !modelConfig.getApiKeyEncrypted().isBlank()) {
            headers.setBearerAuth(modelConfig.getApiKeyEncrypted());
        }
    }

    private String extractDelta(String data) throws Exception {
        JsonNode root = objectMapper.readTree(data);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }

        JsonNode delta = choices.get(0).path("delta");
        JsonNode content = delta.path("content");
        if (content.isTextual()) {
            return content.asText();
        }

        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode node : content) {
                if (node.isTextual()) {
                    sb.append(node.asText());
                } else if (node.has("text") && node.get("text").isTextual()) {
                    sb.append(node.get("text").asText());
                }
            }
            return sb.toString();
        }

        // Compatibility fallback for some gateways.
        if (delta.has("reasoning_content") && delta.get("reasoning_content").isTextual()) {
            return delta.get("reasoning_content").asText();
        }
        return "";
    }
}