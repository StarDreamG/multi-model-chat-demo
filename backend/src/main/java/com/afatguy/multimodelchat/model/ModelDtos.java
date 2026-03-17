package com.afatguy.multimodelchat.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public final class ModelDtos {

    private ModelDtos() {
    }

    public record ModelView(String modelCode, String displayName, boolean enabled, List<String> tags) {
    }

    public record ModelAdminView(
        Long id,
        String modelCode,
        String displayName,
        String providerType,
        String endpoint,
        Integer timeoutMs,
        Integer maxQps,
        boolean enabled,
        List<String> tags,
        OffsetDateTime updatedAt
    ) {
    }

    public record ModelAdminUpsertRequest(
        @NotBlank String modelCode,
        @NotBlank String displayName,
        @NotBlank String providerType,
        @NotBlank String endpoint,
        String apiKey,
        Integer timeoutMs,
        Integer maxQps,
        Boolean enabled,
        List<String> tags
    ) {
    }

    public record EnableRequest(@NotNull Boolean enabled) {
    }

    public record HealthCheckResult(boolean ok, String message, OffsetDateTime checkedAt) {
    }
}