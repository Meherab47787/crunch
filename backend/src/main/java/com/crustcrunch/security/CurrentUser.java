package com.crustcrunch.security;

import com.crustcrunch.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Static access to the authenticated principal (if any). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static AuthUser require() {
        return get().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }
}
