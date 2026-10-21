package com.example.cinerec.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Администратор один, его логин и пароль задаются переменными окружения ADMIN_USERNAME и ADMIN_PASSWORD
 * (см. .env и README). В памяти хранится только хеш пароля (BCrypt).
 * Отдельно от SecurityConfig, чтобы тесты контроллеров могли подключить правила доступа без пароля.
 */
@Configuration
public class AdminUserConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUser(@Value("${cinerec.admin.username}") String username,
                                        @Value("${cinerec.admin.password}") String password,
                                        PasswordEncoder passwordEncoder) {
        if (password.isBlank()) {
            throw new IllegalStateException("Задайте пароль администратора в переменной окружения ADMIN_PASSWORD");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build());
    }
}
