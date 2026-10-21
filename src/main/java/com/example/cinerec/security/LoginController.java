package com.example.cinerec.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Страница входа в админку на Thymeleaf. Форму с неё обрабатывает Spring Security (POST /login). */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
