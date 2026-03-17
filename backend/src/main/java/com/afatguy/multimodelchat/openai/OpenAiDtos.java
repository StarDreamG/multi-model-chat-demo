package com.afatguy.multimodelchat.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OpenAiDtos {

    private OpenAiDtos() {
    }

    public record OpenAiErrorResponse(OpenAiError error) {
    }

    public record OpenAiError(
        String message,
        String type,
        String param,
        String code
    ) {
        public static OpenAiErrorResponse invalidApiKey(String message) {
            return new OpenAiErrorResponse(new OpenAiError(message, "invalid_request_error", null, "invalid_api_key"));
        }

        public static OpenAiErrorResponse invalidRequest(String message) {
            return new OpenAiErrorResponse(new OpenAiError(message, "invalid_request_error", null, "invalid_request"));
        }

        public static OpenAiErrorResponse upstreamError(String message) {
            return new OpenAiErrorResponse(new OpenAiError(message, "server_error", null, "upstream_error"));
        }
    }

    public record ModelsListResponse(
        String object,
        List<ModelObject> data
    ) {
        public static ModelsListResponse of(List<ModelObject> models) {
            return new ModelsListResponse("list", models);
        }
    }

    public record ModelObject(
        String id,
        String object,
        long created,
        @JsonProperty("owned_by") String ownedBy
    ) {
        public static ModelObject of(String id, String ownedBy) {
            return new ModelObject(id, "model", Instant.now().getEpochSecond(), ownedBy);
        }
    }

    public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream,
        Double temperature,
        @JsonProperty("top_p") Double topP,
        @JsonProperty("max_tokens") Integer maxTokens
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatMessage(
        String role,
        Object content
    ) {
        public String contentAsText() {
            if (content == null) {
                return "";
            }
            if (content instanceof String str) {
                return str;
            }
            // Compatibility fallback: allow { "content": [{"type":"text","text":"..."}] } style.
            if (content instanceof List<?> list) {
                StringBuilder sb = new StringBuilder();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Object text = map.get("text");
                        if (text instanceof String t) {
                            sb.append(t);
                        }
                    }
                }
                return sb.toString();
            }
            return String.valueOf(content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionResponse(
        String id,
        String object,
        long created,
        String model,
        List<ChatCompletionChoice> choices,
        Usage usage
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionChoice(
        int index,
        ChatCompletionMessage message,
        @JsonProperty("finish_reason") String finishReason,
        ChatCompletionDelta delta
    ) {
        public static ChatCompletionChoice messageChoice(String content) {
            return new ChatCompletionChoice(0, new ChatCompletionMessage("assistant", content), "stop", null);
        }

        public static ChatCompletionChoice deltaChoice(String delta) {
            return new ChatCompletionChoice(0, null, null, new ChatCompletionDelta(delta));
        }

        public static ChatCompletionChoice doneChoice() {
            return new ChatCompletionChoice(0, null, "stop", new ChatCompletionDelta(null));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionMessage(
        String role,
        String content
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionDelta(
        String content
    ) {
    }

    public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {
        public static Usage of(int promptTokens, int completionTokens) {
            return new Usage(promptTokens, completionTokens, promptTokens + completionTokens);
        }
    }
}

