package com.example.cinerec.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Кто вошёл — для фронтенда. Сам вход и выход (POST /api/auth/login и /api/auth/logout)
 * обрабатывает Spring Security, см. SecurityConfig.
 */
@RestController
public class AuthApiController {

    public record AuthStatus(boolean authenticated, String username) {
    }

    @GetMapping("/api/auth/me")
    public AuthStatus me(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthStatus(false, null);
        }
        return new AuthStatus(true, authentication.getName());
    }
}
