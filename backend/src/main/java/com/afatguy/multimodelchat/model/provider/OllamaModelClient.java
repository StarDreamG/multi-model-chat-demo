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
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaModelClient implements ModelClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaModelClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean supports(String providerType) {
        return "OLLAMA".equalsIgnoreCase(providerType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(ModelConfigEntity modelConfig, ChatCompletionRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelConfig.getModelCode());
        body.put("stream", false);
        body.put("messages", request.messages().stream().map(msg -> Map.of("role", msg.role(), "content", msg.content())).toList());

        Map<String, Object> response = restClient.post()
            .uri(modelConfig.getEndpoint())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return "";
        }

        Map<String, Object> message = (Map<String, Object>) response.get("message");
        if (message != null && message.get("content") != null) {
            return String.valueOf(message.get("content"));
        }

        Object fallback = response.get("response");
        return fallback == null ? "" : String.valueOf(fallback);
    }

    @Override
    public void streamChat(ModelConfigEntity modelConfig, ChatCompletionRequest request, Consumer<String> onDelta) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelConfig.getModelCode());
            body.put("stream", true);
            body.put("messages", request.messages().stream().map(msg -> Map.of("role", msg.role(), "content", msg.content())).toList());

            HttpRequest req = HttpRequest.newBuilder(URI.create(modelConfig.getEndpoint()))
                .timeout(Duration.ofMillis(Math.max(1000, modelConfig.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .header("Accept", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("upstream status=" + response.statusCode() + " body=" + errBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode root = objectMapper.readTree(line);

                    String delta = "";
                    JsonNode message = root.path("message");
                    if (message.has("content") && message.get("content").isTextual()) {
                        delta = message.get("content").asText();
                    } else if (root.has("response") && root.get("response").isTextual()) {
                        delta = root.get("response").asText();
                    }

                    if (!delta.isEmpty()) {
                        onDelta.accept(delta);
                    }

                    if (root.path("done").asBoolean(false)) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("ollama stream parse failed: " + ex.getMessage(), ex);
        }
    }
}