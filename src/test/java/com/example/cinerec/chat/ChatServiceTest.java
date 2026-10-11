package com.example.cinerec.chat;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.ml.ChatMoodPredictor;
import com.example.cinerec.ml.MoodSuggestion;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.Recommendation;
import com.example.cinerec.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Проверка логики бота без базы данных и без обученной модели:
 * репозитории, модель и сервис подбора подменяются заглушками (mock).
 */
class ChatServiceTest {

    private static final long FUNNY = 1L, SAD = 2L;          // настроения в «базе»
    private static final long COMEDY = 10L, HORROR = 11L, HISTORY = 12L; // жанры
    private static final long FILM = 20L, CARTOON = 21L;     // типы

    private final ChatMoodPredictor predictor = mock(ChatMoodPredictor.class);
    private final MoodRepository moodRepository = mock(MoodRepository.class);
    private final GenreRepository genreRepository = mock(GenreRepository.class);
    private final MovieTypeRepository typeRepository = mock(MovieTypeRepository.class);
    private final RecommendationService recommendationService = mock(RecommendationService.class);

    private final ChatService chat = new ChatService(
            predictor, moodRepository, genreRepository, typeRepository, recommendationService);

    @BeforeEach
    void setUp() {
        when(predictor.isAvailable()).thenReturn(true);
        when(predictor.predict(anyString())).thenReturn(Optional.empty());
        when(moodRepository.findAll()).thenReturn(List.of(mood(FUNNY, "весёлое"), mood(SAD, "грустное")));
        when(genreRepository.findAll()).thenReturn(List.of(
                genre(COMEDY, "комедия"), genre(HORROR, "ужасы"), genre(HISTORY, "исторический")));
        when(typeRepository.findAll()).thenReturn(List.of(type(FILM, "фильм"), type(CARTOON, "мультфильм")));
        when(recommendationService.recommend(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void understandsMoodGenreAndTypeAndShowsFiveMovies() {
        modelSays("весёлое", 0.87);
        when(recommendationService.recommend(FUNNY, COMEDY, CARTOON)).thenReturn(recommendations(8));

        ChatReply reply = chat.reply("Хочу посмеяться, посоветуй комедию, лучше мультик");

        verify(recommendationService).recommend(FUNNY, COMEDY, CARTOON);
        assertTrue(reply.text().contains("настроение: весёлое (уверенность 87%)"));
        assertTrue(reply.text().contains("жанр: комедия"));
        assertTrue(reply.text().contains("тип: мультфильм"));
        assertEquals(5, reply.movies().size());
        assertEquals("Фильм 0", reply.movies().get(0).title());
    }

    @Test
    void lowConfidenceMeansMoodNotDetermined() {
        modelSays("грустное", 0.35); // ниже порога 0.4

        ChatReply reply = chat.reply("посоветуй ужастик");

        verify(recommendationService).recommend(null, HORROR, null);
        assertTrue(reply.text().contains("настроение: не определено"));
        assertTrue(reply.text().contains("жанр: ужасы"));
    }

    @Test
    void moodMissingInDatabaseIsReportedAndIgnored() {
        modelSays("мрачное", 0.9); // такого настроения нет в «базе»

        ChatReply reply = chat.reply("хочу мрачную комедию");

        verify(recommendationService).recommend(null, COMEDY, null);
        assertTrue(reply.text().contains("Настроения «мрачное» нет в базе"));
    }

    @Test
    void nothingUnderstoodAsksToRephraseWithExamples() {
        ChatReply reply = chat.reply("ну такое");

        verify(recommendationService, never()).recommend(any(), any(), any());
        assertTrue(reply.text().contains("Опишите иначе"));
        assertTrue(reply.text().contains("«хочу посмеяться»"));
        assertTrue(reply.movies().isEmpty());
    }

    @Test
    void greetingAndThanksGetShortAnswerWithoutRecommendations() {
        assertTrue(chat.reply("Привет!").text().startsWith("Привет!"));
        assertTrue(chat.reply("Добрый вечер").text().startsWith("Привет!"));
        assertTrue(chat.reply("спасибо большое)").text().startsWith("Пожалуйста"));

        verify(predictor, never()).predict(anyString());
        verify(recommendationService, never()).recommend(any(), any(), any());
    }

    @Test
    void genreAndTypeAreFoundByWordStem() {
        assertEquals("комедия", ChatService.findInMessage(
                ChatService.words("найди комедию"), genreRepository.findAll(), Genre::getName).getName());
        assertEquals("ужасы", ChatService.findInMessage(
                ChatService.words("хочу ужастик"), genreRepository.findAll(), Genre::getName).getName());
        assertEquals("мультфильм", ChatService.findInMessage(
                ChatService.words("какой-нибудь мультик"), typeRepository.findAll(), MovieType::getName).getName());
        assertEquals("фильм", ChatService.findInMessage(
                ChatService.words("посоветуй фильмы"), typeRepository.findAll(), MovieType::getName).getName());
        // «историю» не должна считаться жанром «исторический»
        assertNull(ChatService.findInMessage(
                ChatService.words("хочу историю успеха"), genreRepository.findAll(), Genre::getName));
    }

    @Test
    void untrainedModelIsReported() {
        when(predictor.isAvailable()).thenReturn(false);

        ChatReply reply = chat.reply("хочу комедию");

        assertTrue(reply.text().contains("не обучена"));
        assertTrue(reply.text().contains("./mvnw compile exec:java"));
    }

    private void modelSays(String mood, double probability) {
        when(predictor.predict(anyString())).thenReturn(Optional.of(new MoodSuggestion(mood, probability)));
    }

    private static List<Recommendation> recommendations(int count) {
        MovieType type = type(FILM, "фильм");
        List<Recommendation> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Movie movie = new Movie();
            movie.setId((long) i);
            movie.setTitle("Фильм " + i);
            movie.setRating(8.0);
            movie.setType(type);
            result.add(new Recommendation(movie, 6, true, true, true));
        }
        return result;
    }

    private static Mood mood(long id, String name) {
        Mood mood = new Mood();
        mood.setId(id);
        mood.setName(name);
        return mood;
    }

    private static Genre genre(long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }

    private static MovieType type(long id, String name) {
        MovieType type = new MovieType();
        type.setId(id);
        type.setName(name);
        return type;
    }
}
