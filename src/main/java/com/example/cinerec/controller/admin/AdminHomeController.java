package com.example.cinerec.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    // Вход в админку сразу ведёт к списку фильмов
    @GetMapping("/admin")
    public String home() {
        return "redirect:/admin/movies";
    }
}
