package com.afatguy.multimodelchat.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentPrincipal().userId();
    }
}