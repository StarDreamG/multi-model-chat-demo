package com.afatguy.multimodelchat.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record ChatMessageInput(@NotBlank String role, @NotBlank String content) {
    }

    public record ChatCompletionRequest(
        @NotNull Long sessionId,
        @NotBlank String modelCode,
        @NotEmpty List<ChatMessageInput> messages,
        Double temperature,
        Double topP,
        Integer maxTokens
    ) {
    }

    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
    }

    public record ChatCompletionResponse(String requestId, com.afatguy.multimodelchat.session.SessionDtos.ChatMessageView message, TokenUsage usage) {
    }

    public record StreamPayload(String requestId, String modelCode, String fullText) {
    }
}