package com.example.cinerec.controller;

import com.example.cinerec.chat.ChatContext;
import com.example.cinerec.chat.ChatMovie;
import com.example.cinerec.chat.ChatReply;
import com.example.cinerec.chat.ChatService;
import com.example.cinerec.ml.ChatMoodPredictor;
import com.example.cinerec.service.Criteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Чат на уровне HTTP: как JSON со страницы превращается в вызов ChatService и обратно.
 * Логика бота подменена заглушкой — её проверяет ChatServiceTest.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatMoodPredictor chatMoodPredictor;

    @Test
    void messageAndContextGoToBotAndReplyComesBackAsJson() throws Exception {
        Criteria nineties = new Criteria(1L, Set.of(2L), Set.of(), Set.of(), Set.of(), 1990, 1999);
        when(chatService.reply(eq("ещё"), any())).thenReturn(new ChatReply("Ещё варианты:",
                List.of(new ChatMovie(7L, "Амели", 2001, "фильм", 8.3, null)),
                new ChatContext(nineties, List.of(3L, 4L, 7L))));

        // Так отправляет chat.js: контекст из прошлого ответа, пропущенные поля — пустые
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("""
                        {"message": "  ещё  ",
                         "context": {"criteria": {"moodId": 1, "genreIds": [2], "yearFrom": 1990, "yearTo": 1999},
                                     "shownMovieIds": [3, 4]}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies[0].id").value(7))
                .andExpect(jsonPath("$.movies[0].year").value(2001))
                .andExpect(jsonPath("$.context.criteria.moodId").value(1))
                .andExpect(jsonPath("$.context.criteria.yearTo").value(1999))
                .andExpect(jsonPath("$.context.shownMovieIds.length()").value(3));

        verify(chatService).reply("ещё", new ChatContext(nineties, List.of(3L, 4L)));
    }

    @Test
    void firstMessageHasNoContext() throws Exception {
        when(chatService.reply(any(), any())).thenReturn(ChatReply.textOnly("Привет!"));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"привет\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies.length()").value(0))
                .andExpect(jsonPath("$.context").isEmpty());

        verify(chatService).reply("привет", null);
    }

    @Test
    void tooLongMessageIsRejectedWithoutCallingBot() throws Exception {
        String message = "а".repeat(ChatController.MAX_MESSAGE_LENGTH + 1);

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"" + message + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", containsString("слишком длинное")))
                .andExpect(jsonPath("$.movies.length()").value(0));

        verifyNoInteractions(chatService);
    }

    @Test
    void chatPageWarnsWhenModelIsNotTrained() throws Exception {
        when(chatMoodPredictor.isAvailable()).thenReturn(false);

        mvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(content().string(containsString("Модель для чата не обучена")));
    }
}
