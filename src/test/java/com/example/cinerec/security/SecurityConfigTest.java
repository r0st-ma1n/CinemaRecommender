package com.example.cinerec.security;

import com.example.cinerec.controller.admin.AdminGenreController;
import com.example.cinerec.controller.api.AdminGenreApiController;
import com.example.cinerec.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Кто куда может зайти: админка закрыта, вход и выход через API, защита от CSRF. */
@WebMvcTest({AuthApiController.class, AdminGenreApiController.class, AdminGenreController.class})
@Import({SecurityConfig.class, AdminUserConfig.class})
@TestPropertySource(properties = {"cinerec.admin.username=admin", "cinerec.admin.password=secret"})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GenreRepository genreRepository;

    @Test
    void adminPagesRedirectGuestToLoginPage() throws Exception {
        mvc.perform(get("/admin/genres"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void adminApiGives401WithMessageToGuest() throws Exception {
        mvc.perform(post("/api/admin/genres").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"вестерн\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Войдите как администратор"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminApiRequestWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/admin/genres")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"вестерн\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Сессия устарела, обновите страницу"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userWithoutAdminRoleGets403() throws Exception {
        mvc.perform(post("/api/admin/genres").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"вестерн\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Недостаточно прав"));
    }

    // Свежий контекст: csrf() из других тестов подменяет хранилище токенов внутри CsrfFilter,
    // и после этого cookie XSRF-TOKEN в ответ не пишется
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void guestIsNotAuthenticatedAndGetsCsrfCookie() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(header().string("Set-Cookie", startsWith("XSRF-TOKEN=")));
    }

    @Test
    void loginWithWrongPasswordGives401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "admin").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
    }

    @Test
    void afterLoginSessionKnowsAdminAndLogoutForgetsIt() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "admin").param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn().getRequest().getSession();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"));

        mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
