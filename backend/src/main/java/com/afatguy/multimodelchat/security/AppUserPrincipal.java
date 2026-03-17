package com.afatguy.multimodelchat.security;

import java.util.List;

public record AppUserPrincipal(Long userId, String username, String displayName, List<String> roles) {
}