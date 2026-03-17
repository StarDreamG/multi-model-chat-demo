package com.afatguy.multimodelchat.session;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

public final class SessionDtos {

    private SessionDtos() {
    }

    public record SessionView(Long id, String title, String modelCode, OffsetDateTime updatedAt) {
    }

    public record SessionPage(List<SessionView> items, int page, int pageSize, long total) {
    }

    public record CreateSessionRequest(@NotBlank String title, @NotBlank String modelCode) {
    }

    public record UpdateSessionRequest(String title, String modelCode) {
    }

    public record ChatMessageView(Long id, String role, String content, String modelCode, OffsetDateTime createdAt) {
    }
}