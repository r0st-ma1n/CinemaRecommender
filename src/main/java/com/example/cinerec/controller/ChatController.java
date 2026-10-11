package com.example.cinerec.controller;

import com.example.cinerec.chat.ChatReply;
import com.example.cinerec.chat.ChatService;
import com.example.cinerec.ml.ChatMoodPredictor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Чат-бот: страница /chat и адрес /api/chat, на который скрипт страницы отправляет сообщения.
 * История переписки хранится только на странице, в базу ничего не пишется.
 */
@Controller
public class ChatController {

    static final int MAX_MESSAGE_LENGTH = 500;

    private final ChatService chatService;
    private final ChatMoodPredictor chatMoodPredictor;

    public ChatController(ChatService chatService, ChatMoodPredictor chatMoodPredictor) {
        this.chatService = chatService;
        this.chatMoodPredictor = chatMoodPredictor;
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("modelAvailable", chatMoodPredictor.isAvailable());
        return "chat";
    }

    /** Тело запроса: {"message": "хочу посмеяться"}. */
    public record ChatRequest(String message) {
    }

    /**
     * Принимает сообщение в JSON и возвращает ответ бота в JSON:
     * {"text": "...", "movies": [{"id": 1, "title": "...", ...}]}.
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ChatReply sendMessage(@RequestBody ChatRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ChatReply.textOnly("Сообщение слишком длинное. Опишите желание короче, до "
                    + MAX_MESSAGE_LENGTH + " символов.");
        }
        return chatService.reply(message);
    }
}
