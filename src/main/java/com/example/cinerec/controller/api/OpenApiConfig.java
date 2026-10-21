package com.example.cinerec.controller.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Описание API для Swagger UI (/swagger-ui.html).
 * Вход и выход обрабатывает Spring Security, а не контроллер, поэтому springdoc их не видит —
 * они добавляются вручную. Адреса /api/admin/** помечаются как требующие входа.
 */
@Configuration
public class OpenApiConfig {

    private static final String SESSION = "session";
    private static final String AUTH_TAG = "Вход";

    @Bean
    public OpenAPI cinerecOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CinemaRecommender API")
                        .version("1.0")
                        .description("""
                                Подбор фильмов по настроению, жанру и типу, чат-бот и админка.
                                Ошибки возвращаются как {"message": "..."}.
                                Чтобы вызвать адреса админки, сначала выполните POST /api/auth/login."""))
                .components(new Components().addSecuritySchemes(SESSION, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("Сессия появляется после входа через POST /api/auth/login")));
    }

    @Bean
    public OpenApiCustomizer authEndpoints() {
        return openApi -> {
            openApi.path("/api/auth/login", new PathItem().post(new Operation()
                    .tags(List.of(AUTH_TAG))
                    .summary("Вход администратора")
                    .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                            "application/x-www-form-urlencoded",
                            new MediaType().schema(new ObjectSchema()
                                    .addProperty("username", new StringSchema())
                                    .addProperty("password", new StringSchema().format("password"))
                                    .required(List.of("username", "password"))))))
                    .responses(new ApiResponses()
                            .addApiResponse("200", new ApiResponse().description("Вход выполнен, сессия в cookie"))
                            .addApiResponse("401", new ApiResponse().description("Неверный логин или пароль")))));

            openApi.path("/api/auth/logout", new PathItem().post(new Operation()
                    .tags(List.of(AUTH_TAG))
                    .summary("Выход")
                    .responses(new ApiResponses()
                            .addApiResponse("204", new ApiResponse().description("Сессия завершена")))));

            openApi.getPaths().forEach((path, item) -> {
                if (path.startsWith("/api/admin/")) {
                    item.readOperations().forEach(operation -> operation
                            .addSecurityItem(new SecurityRequirement().addList(SESSION))
                            .getResponses()
                            .addApiResponse("401", new ApiResponse().description("Нужно войти как администратор")));
                }
            });
        };
    }
}
