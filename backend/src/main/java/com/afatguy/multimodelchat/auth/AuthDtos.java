package com.afatguy.multimodelchat.auth;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserProfile(Long id, String username, String displayName, List<String> roles) {
    }

    public record LoginResponse(String token, OffsetDateTime expiresAt, UserProfile user) {
    }
}