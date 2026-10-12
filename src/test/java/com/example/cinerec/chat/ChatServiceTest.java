package com.example.cinerec.chat;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.ml.ChatMoodPredictor;
import com.example.cinerec.ml.MoodSuggestion;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.Criteria;
import com.example.cinerec.service.Recommendation;
import com.example.cinerec.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private static final long SCI_FI = 13L, ADVENTURE = 14L, WESTERN = 15L;
    private static final long FILM = 20L, CARTOON = 21L;     // типы

    private final ChatMoodPredictor predictor = mock(ChatMoodPredictor.class);
    private final MoodRepository moodRepository = mock(MoodRepository.class);
    private final GenreRepository genreRepository = mock(GenreRepository.class);
    private final MovieTypeRepository typeRepository = mock(MovieTypeRepository.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final RecommendationService recommendationService = mock(RecommendationService.class);

    private final ChatService chat = new ChatService(
            predictor, moodRepository, genreRepository, typeRepository, movieRepository, recommendationService);

    @BeforeEach
    void setUp() {
        when(predictor.isAvailable()).thenReturn(true);
        when(predictor.predict(anyString())).thenReturn(Optional.empty());
        when(moodRepository.findAll()).thenReturn(List.of(mood(FUNNY, "весёлое"), mood(SAD, "грустное")));
        when(genreRepository.findAll()).thenReturn(List.of(
                genre(COMEDY, "комедия"), genre(HORROR, "ужасы"), genre(HISTORY, "исторический"),
                genre(SCI_FI, "фантастика"), genre(ADVENTURE, "приключения"),
                genre(WESTERN, "вестерн"))); // вестерна нет в KEYWORDS — ищется по основе слова
        when(typeRepository.findAll()).thenReturn(List.of(type(FILM, "фильм"), type(CARTOON, "мультфильм")));
        when(recommendationService.rank(any())).thenReturn(List.of());
    }

    @Test
    void understandsMoodGenreAndTypeAndShowsFiveMovies() {
        modelSays("весёлое", 0.87);
        when(recommendationService.rank(criteria(FUNNY, Set.of(COMEDY), Set.of(CARTOON), Set.of(), Set.of()))).thenReturn(recommendations(8));

        ChatReply reply = chat.reply("Хочу посмеяться, посоветуй комедию, лучше мультик");

        verify(recommendationService).rank(criteria(FUNNY, Set.of(COMEDY), Set.of(CARTOON), Set.of(), Set.of()));
        assertTrue(reply.text().contains("настроение: весёлое (уверенность 87%)"));
        assertTrue(reply.text().contains("жанр: комедия"));
        assertTrue(reply.text().contains("тип: мультфильм"));
        assertEquals(5, reply.movies().size());
        assertEquals("Фильм 0", reply.movies().get(0).title());
    }

    @Test
    void lowConfidenceMeansMoodNotDetermined() {
        modelSays("грустное", 0.45); // ниже порога 0.5

        ChatReply reply = chat.reply("посоветуй ужастик");

        verify(recommendationService).rank(criteria(null, Set.of(HORROR), Set.of(), Set.of(), Set.of()));
        assertTrue(reply.text().contains("настроение: не определено"));
        assertTrue(reply.text().contains("жанр: ужасы"));
    }

    @Test
    void moodMissingInDatabaseIsReportedAndIgnored() {
        modelSays("мрачное", 0.9); // такого настроения нет в «базе»

        ChatReply reply = chat.reply("хочу мрачную комедию");

        verify(recommendationService).rank(criteria(null, Set.of(COMEDY), Set.of(), Set.of(), Set.of()));
        assertTrue(reply.text().contains("Настроения «мрачное» нет в базе"));
    }

    @Test
    void nothingUnderstoodAsksToRephraseWithExamples() {
        ChatReply reply = chat.reply("ну такое");

        verify(recommendationService, never()).rank(any());
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
        verify(recommendationService, never()).rank(any());
    }

    @Test
    void genreAndTypeAreFoundByWordStem() {
        assertEquals("комедия", ChatService.findInMessage(
                ChatService.words("найди комедию"), genreRepository.findAll(), Genre::getName).wanted().get(0).getName());
        assertEquals("ужасы", ChatService.findInMessage(
                ChatService.words("хочу ужастик"), genreRepository.findAll(), Genre::getName).wanted().get(0).getName());
        assertEquals("мультфильм", ChatService.findInMessage(
                ChatService.words("какой-нибудь мультик"), typeRepository.findAll(), MovieType::getName).wanted().get(0).getName());
        assertEquals("фильм", ChatService.findInMessage(
                ChatService.words("посоветуй фильмы"), typeRepository.findAll(), MovieType::getName).wanted().get(0).getName());
        // «историю» не должна считаться жанром «исторический»
        assertTrue(ChatService.findInMessage(
                ChatService.words("хочу историю успеха"), genreRepository.findAll(), Genre::getName).wanted().isEmpty());
    }

    @Test
    void genreWordFormsFromKeywordsAndStemFallback() {
        assertEquals(SCI_FI, wantedGenre("что-нибудь фантастическое").getId());
        assertEquals(ADVENTURE, wantedGenre("приключенческий фильм").getId());
        assertEquals(HORROR, wantedGenre("фильм ужасов").getId());
        assertEquals(HORROR, wantedGenre("какой-нибудь хоррор").getId());
        assertEquals(WESTERN, wantedGenre("старые вестерны").getId());
        // «ужасно» — не жанр «ужасы»
        assertNull(wantedGenre("ужасно устал после работы"));
    }

    @Test
    void negatedGenresAndTypesAreExcluded() {
        modelSays("весёлое", 0.8);

        ChatReply reply = chat.reply("хочу посмеяться, только не ужасы и не хочу мультики");

        verify(recommendationService).rank(criteria(FUNNY, Set.of(), Set.of(), Set.of(HORROR), Set.of(CARTOON)));
        assertTrue(reply.text().contains("жанр: любой"));
        assertTrue(reply.text().contains("кроме: ужасы, мультфильм"));
    }

    @Test
    void severalGenresAndTypesAreAllUsed() {
        ChatReply reply = chat.reply("посоветуй фантастику или комедию, фильм или мультик");

        verify(recommendationService).rank(criteria(null, Set.of(SCI_FI, COMEDY), Set.of(FILM, CARTOON), Set.of(), Set.of()));
        assertTrue(reply.text().contains("жанр: комедия или фантастика"));
        assertTrue(reply.text().contains("тип: фильм или мультфильм"));
    }

    @Test
    void refinementReplacesGenresOrRemovesExcludedOne() {
        ChatContext previous = new ChatContext(criteria(FUNNY, Set.of(COMEDY, SCI_FI), Set.of(), Set.of(), Set.of()), List.of());

        chat.reply("а теперь приключения", previous);
        verify(recommendationService).rank(criteria(FUNNY, Set.of(ADVENTURE), Set.of(), Set.of(), Set.of()));

        chat.reply("только не комедии", previous);
        verify(recommendationService).rank(criteria(FUNNY, Set.of(SCI_FI), Set.of(), Set.of(COMEDY), Set.of()));
    }

    @Test
    void negationWorksOnlyRightBeforeWordOrThroughVerb() {
        var genres = genreRepository.findAll();

        var withoutHorror = ChatService.findInMessage(ChatService.words("комедию, но без ужасов"), genres, Genre::getName);
        assertEquals(COMEDY, withoutHorror.wanted().get(0).getId());
        assertEquals(List.of(HORROR), withoutHorror.excluded().stream().map(Genre::getId).toList());

        // «не» относится к «грустную», а комедия нужна
        var notSadComedy = ChatService.findInMessage(ChatService.words("не грустную комедию"), genres, Genre::getName);
        assertEquals(COMEDY, notSadComedy.wanted().get(0).getId());
        assertTrue(notSadComedy.excluded().isEmpty());
    }

    @Test
    void onlyExclusionsAskWhatToWatch() {
        ChatReply reply = chat.reply("только не ужасы");

        verify(recommendationService, never()).rank(any());
        assertTrue(reply.text().startsWith("Понял, что не хотите: ужасы."));
        assertTrue(reply.movies().isEmpty());
    }

    @Test
    void moreShowsNextMoviesWithoutRepeats() {
        modelSays("весёлое", 0.87);
        when(recommendationService.rank(criteria(FUNNY, Set.of(COMEDY), Set.of(), Set.of(), Set.of()))).thenReturn(recommendations(8));

        ChatReply first = chat.reply("хочу посмеяться, комедию");
        assertEquals(List.of(0L, 1L, 2L, 3L, 4L), first.context().shownMovieIds());
        assertTrue(first.text().contains("напишите «ещё»"));

        ChatReply second = chat.reply("Ещё!", first.context());
        assertEquals(List.of("Фильм 5", "Фильм 6", "Фильм 7"), titles(second));
        assertTrue(second.text().startsWith("Ещё варианты:"));

        ChatReply third = chat.reply("а что-нибудь другое есть?", second.context());
        assertTrue(third.movies().isEmpty());
        assertTrue(third.text().startsWith("Больше подходящих фильмов нет"));
        verify(predictor, times(1)).predict(anyString()); // «ещё» модель не спрашивает
    }

    @Test
    void moreWithoutPreviousRequestAsksWhatToWatch() {
        ChatReply reply = chat.reply("ещё", null);

        assertTrue(reply.text().startsWith("Сначала расскажите"));
        assertNull(reply.context());
    }

    @Test
    void refinementChangesOnlyNewConditions() {
        ChatContext previous = new ChatContext(criteria(FUNNY, Set.of(COMEDY), Set.of(FILM), Set.of(HORROR), Set.of()), List.of(0L, 1L));

        ChatReply reply = chat.reply("а мультик?", previous);

        verify(recommendationService).rank(criteria(FUNNY, Set.of(COMEDY), Set.of(CARTOON), Set.of(HORROR), Set.of()));
        assertTrue(reply.text().contains("настроение: весёлое"));
        assertTrue(reply.text().contains("тип: мультфильм"));
        assertTrue(reply.context().shownMovieIds().isEmpty()); // новые условия — показ заново
    }

    @Test
    void refinementWithExclusionRemovesPreviousWish() {
        ChatContext previous = new ChatContext(criteria(FUNNY, Set.of(HORROR), Set.of(), Set.of(), Set.of()), List.of());
        modelSays("грустное", 0.9); // по слову «ужасы» модель угадала бы настроение — её не спрашиваем

        chat.reply("только не ужасы, пожалуйста", previous);

        verify(predictor, never()).predict(anyString());
        verify(recommendationService).rank(criteria(FUNNY, Set.of(), Set.of(), Set.of(HORROR), Set.of()));
    }

    @Test
    void newRequestWithoutRefineWordStartsOver() {
        ChatContext previous = new ChatContext(criteria(FUNNY, Set.of(COMEDY), Set.of(CARTOON), Set.of(), Set.of()), List.of());

        chat.reply("посоветуй ужастик", previous);

        verify(recommendationService).rank(criteria(null, Set.of(HORROR), Set.of(), Set.of(), Set.of()));
    }

    @Test
    void yearsAreUnderstoodInDifferentForms() {
        assertEquals(new YearRange(1990, 1999), years("комедии 90-х"));
        assertEquals(new YearRange(1980, 1989), years("что-нибудь из 1980-х"));
        assertEquals(new YearRange(2000, 2009), years("сериалы 2000-х")); // не «2000 год»
        assertEquals(new YearRange(2000, 2009), years("фильмы нулевых"));
        assertEquals(new YearRange(1990, 1999), years("кино девяностых"));
        assertEquals(new YearRange(2011, null), years("снятое после 2010"));
        assertEquals(new YearRange(1990, 1999), years("с 1990 по 1999"));
        assertEquals(new YearRange(null, 1999), years("до 2000 года"));
        assertEquals(new YearRange(1994, 1994), years("фильм 1994 года"));
        assertEquals(new YearRange(1994, 1994), years("1994"));
        // «новое» и «старое» — от самого нового фильма в базе (2020)
        assertEquals(new YearRange(2011, null), years("что-нибудь новое"));
        assertEquals(new YearRange(null, 1990), years("старое доброе кино"));
        assertEquals(new YearRange(2011, null), years("только не старое"));
        assertNull(years("фильм на новый год"));
        assertNull(years("хочу посмеяться"));
    }

    @Test
    void yearsFilterRecommendations() {
        when(movieRepository.findNewestYear()).thenReturn(2020);
        modelSays("весёлое", 0.9);

        ChatReply reply = chat.reply("хочу новую комедию");

        verify(recommendationService).rank(new Criteria(FUNNY, Set.of(COMEDY), Set.of(), Set.of(), Set.of(), 2011, null));
        assertTrue(reply.text().contains("годы: не раньше 2011"));
    }

    @Test
    void onlyYearsAreEnoughToRecommend() {
        ChatReply reply = chat.reply("что-нибудь из 90-х");

        verify(recommendationService).rank(new Criteria(null, Set.of(), Set.of(), Set.of(), Set.of(), 1990, 1999));
        assertTrue(reply.text().contains("годы: 1990–1999"));
        verify(movieRepository, never()).findNewestYear(); // самый новый год нужен только для «новое» и «старое»
    }

    @Test
    void yearsRefinePreviousRequest() {
        ChatContext previous = new ChatContext(criteria(FUNNY, Set.of(COMEDY), Set.of(), Set.of(), Set.of()), List.of());

        chat.reply("а из 90-х?", previous);

        verify(recommendationService).rank(new Criteria(FUNNY, Set.of(COMEDY), Set.of(), Set.of(), Set.of(), 1990, 1999));
    }

    private static YearRange years(String message) {
        return YearRange.parse(message, ChatService.words(message), () -> 2020);
    }

    /** Условия без годов. */
    private static Criteria criteria(Long moodId, Set<Long> genreIds, Set<Long> typeIds,
                                     Set<Long> excludedGenreIds, Set<Long> excludedTypeIds) {
        return new Criteria(moodId, genreIds, typeIds, excludedGenreIds, excludedTypeIds, null, null);
    }

    private static List<String> titles(ChatReply reply) {
        return reply.movies().stream().map(ChatMovie::title).toList();
    }

    private Genre wantedGenre(String message) {
        return ChatService.findInMessage(ChatService.words(message), genreRepository.findAll(), Genre::getName).wanted()
                .stream().findFirst().orElse(null);
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
