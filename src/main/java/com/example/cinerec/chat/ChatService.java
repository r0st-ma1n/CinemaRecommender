package com.example.cinerec.chat;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.ml.ChatMoodPredictor;
import com.example.cinerec.ml.MoodSuggestion;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.Recommendation;
import com.example.cinerec.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Логика чат-бота. По сообщению пользователя определяет:
 * - настроение — моделью для чата (если она уверена хотя бы на MOOD_THRESHOLD);
 * - жанр и тип — по основе слова из таблиц genre и type («комедию» → комедия);
 * затем подбирает фильмы через RecommendationService и собирает ответ по шаблону.
 */
@Service
public class ChatService {

    /**
     * Порог уверенности модели. Выбран по таблице, которую печатает обучение (TrainChatModel):
     * на 600 фразах при 0.4 настроение определяется примерно в 80% фраз и верно примерно в 77% из них.
     * При 0.3 бот чаще ошибается, при высоком пороге — слишком часто переспрашивает.
     */
    static final double MOOD_THRESHOLD = 0.4;
    static final int MAX_MOVIES = 5;

    // Основа слова — название без двух последних букв, но не короче 4 букв:
    // «комедия» → «комед» (подходит «комедию»), «ужасы» → «ужас» (подходит «ужастик»).
    private static final int MIN_STEM_LENGTH = 4;

    // Разговорные слова, которые не начинаются с основы названия: «мультик» → мультфильм
    private static final Map<String, String> SLANG = Map.of(
            "мульт", "мультфильм",
            "хоррор", "ужасы");

    private static final Set<String> GREETING_WORDS = Set.of(
            "привет", "приветик", "приветствую", "здравствуй", "здравствуйте", "здрасте",
            "хай", "хей", "добрый", "доброе", "день", "вечер", "утро", "бот", "hello", "hi");
    private static final Set<String> THANKS_WORDS = Set.of(
            "спасибо", "спасибочки", "спс", "пасиб", "благодарю", "thanks");
    // Слова, которые часто стоят рядом со «спасибо»: «спасибо тебе большое»
    private static final Set<String> THANKS_FILLER = Set.of("тебе", "вам", "большое", "огромное", "бот");

    static final String EXAMPLES = "Например:\n• «хочу посмеяться»\n• «посоветуй ужастик»\n• «грустный сериал»";

    private final ChatMoodPredictor moodPredictor;
    private final MoodRepository moodRepository;
    private final GenreRepository genreRepository;
    private final MovieTypeRepository typeRepository;
    private final RecommendationService recommendationService;

    public ChatService(ChatMoodPredictor moodPredictor,
                       MoodRepository moodRepository,
                       GenreRepository genreRepository,
                       MovieTypeRepository typeRepository,
                       RecommendationService recommendationService) {
        this.moodPredictor = moodPredictor;
        this.moodRepository = moodRepository;
        this.genreRepository = genreRepository;
        this.typeRepository = typeRepository;
        this.recommendationService = recommendationService;
    }

    public ChatReply reply(String message) {
        List<String> words = words(message);

        // 1. Приветствие и «спасибо» — короткий ответ без подбора
        if (words.isEmpty()) {
            return ChatReply.textOnly("Напишите, что хотите посмотреть. " + EXAMPLES);
        }
        if (words.stream().anyMatch(THANKS_WORDS::contains)
                && words.stream().allMatch(w -> THANKS_WORDS.contains(w) || THANKS_FILLER.contains(w))) {
            return ChatReply.textOnly("Пожалуйста! Обращайтесь, если захотите ещё что-нибудь посмотреть.");
        }
        if (words.stream().allMatch(GREETING_WORDS::contains)) {
            return ChatReply.textOnly("Привет! Расскажите, какое у вас настроение или что хочется посмотреть.");
        }

        if (!moodPredictor.isAvailable()) {
            return ChatReply.textOnly("Модель для чата не обучена. Выполните команду "
                    + "./mvnw compile exec:java и перезапустите приложение.");
        }

        // 2. Что понял бот. Справочники читаются из базы при каждом сообщении,
        //    поэтому новые жанры и типы из админки сразу понимаются чатом.
        MoodSuggestion mood = moodPredictor.predict(message)
                .filter(p -> p.probability() >= MOOD_THRESHOLD)
                .orElse(null);
        Genre genre = findInMessage(words, genreRepository.findAll(), Genre::getName);
        MovieType type = findInMessage(words, typeRepository.findAll(), MovieType::getName);

        if (mood == null && genre == null && type == null) {
            return ChatReply.textOnly("Не совсем понял, что вам хочется посмотреть. Опишите иначе. " + EXAMPLES);
        }

        // Настроение модели ищем в таблице mood по названию — его могли переименовать или удалить в админке
        Mood moodInDb = mood == null ? null : moodRepository.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(mood.mood()))
                .findFirst()
                .orElse(null);

        StringBuilder text = new StringBuilder("Понял так:\n");
        text.append("• настроение: ").append(mood == null ? "не определено"
                : mood.mood() + " (уверенность " + mood.percent() + "%)").append('\n');
        text.append("• жанр: ").append(genre == null ? "любой" : genre.getName()).append('\n');
        text.append("• тип: ").append(type == null ? "любой" : type.getName()).append('\n');

        if (mood != null && moodInDb == null) {
            text.append("Настроения «").append(mood.mood())
                    .append("» нет в базе, поэтому подбираю только по жанру и типу.\n");
            if (genre == null && type == null) {
                text.append("Уточните жанр или тип. ").append(EXAMPLES);
                return ChatReply.textOnly(text.toString());
            }
        }

        // 3. Подбор существующим сервисом: те же баллы, что и в форме подбора
        List<Recommendation> recommendations = recommendationService.recommend(
                moodInDb == null ? null : moodInDb.getId(),
                genre == null ? null : genre.getId(),
                type == null ? null : type.getId());
        List<ChatMovie> movies = recommendations.stream()
                .limit(MAX_MOVIES)
                .map(r -> ChatMovie.from(r.movie()))
                .toList();

        text.append(movies.isEmpty() ? "Подходящих фильмов не нашлось. Попробуйте описать иначе."
                : "Вот что могу предложить:");
        return new ChatReply(text.toString(), movies);
    }

    /** Слова сообщения в нижнем регистре, «ё» заменена на «е», без знаков препинания. */
    static List<String> words(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalize(message).split("[^\\p{L}]+"))
                .filter(w -> !w.isEmpty())
                .toList();
    }

    /**
     * Первое слово сообщения, которое начинается с основы названия из справочника
     * (или является разговорным вариантом из SLANG). Возвращает найденную запись или null.
     */
    static <T> T findInMessage(List<String> words, List<T> items, Function<T, String> nameOf) {
        for (String word : words) {
            String slangName = SLANG.entrySet().stream()
                    .filter(e -> word.startsWith(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            for (T item : items) {
                String name = normalize(nameOf.apply(item));
                if (word.startsWith(stem(name)) || name.equals(slangName)) {
                    return item;
                }
            }
        }
        return null;
    }

    static String stem(String name) {
        int length = Math.max(MIN_STEM_LENGTH, name.length() - 2);
        return name.substring(0, Math.min(length, name.length()));
    }

    private static String normalize(String text) {
        return text.toLowerCase().replace('ё', 'е');
    }
}
