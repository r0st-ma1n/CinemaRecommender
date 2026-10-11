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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Логика чат-бота. По сообщению пользователя определяет:
 * - настроение — моделью для чата (если она уверена хотя бы на MOOD_THRESHOLD);
 * - жанр и тип — по словам из KEYWORDS или по основе слова из таблиц genre и type («комедию» → комедия);
 * - что смотреть НЕ нужно — жанры и типы с отрицанием перед ними («только не ужасы»);
 * затем подбирает фильмы через RecommendationService и собирает ответ по шаблону.
 */
@Service
public class ChatService {

    /**
     * Порог уверенности модели. Выбран по таблице, которую печатает обучение (TrainChatModel):
     * на 600 фразах при 0.5 настроение определяется примерно в 65% фраз и верно примерно в 84% из них.
     * При 0.4 бот определяет больше (79%), но чаще ошибается (верно 77%); при 0.6 и выше —
     * переспрашивает почти в половине случаев. Лучше переспросить, чем уверенно подобрать не то.
     */
    static final double MOOD_THRESHOLD = 0.5;
    static final int MAX_MOVIES = 5;

    // Начала слов, по которым узнаются жанры и типы из начальных данных:
    // «фантастический» → фантастика, «мультик» → мультфильм, «про войну» → военный.
    // Отдельные списки нужны, потому что основа слова то ловит лишнее («ужасно» → ужасы),
    // то пропускает нужное («приключенческий» не начинается с «приключени»).
    private static final Map<String, List<String>> KEYWORDS = Map.ofEntries(
            Map.entry("драма", List.of("драм")),
            Map.entry("комедия", List.of("комед")),
            Map.entry("боевик", List.of("боевик", "экшн", "экшен")),
            Map.entry("триллер", List.of("триллер", "трилер")),
            Map.entry("фантастика", List.of("фантаст")),
            Map.entry("фэнтези", List.of("фэнтез", "фентез")),
            Map.entry("ужасы", List.of("ужасы", "ужасов", "ужасам", "ужасах", "ужастик", "хоррор")),
            Map.entry("мелодрама", List.of("мелодрам")),
            Map.entry("приключения", List.of("приключен")),
            Map.entry("детектив", List.of("детектив")),
            Map.entry("криминал", List.of("криминал")),
            Map.entry("семейный", List.of("семейн")),
            Map.entry("исторический", List.of("историческ")),
            Map.entry("военный", List.of("военн", "войн")),
            Map.entry("биография", List.of("биограф")),
            Map.entry("мюзикл", List.of("мюзикл")),
            Map.entry("фильм", List.of("фильм")),
            Map.entry("сериал", List.of("сериал")),
            Map.entry("мультфильм", List.of("мульт")));

    // Названия, которых нет в KEYWORDS (например, новый жанр из админки), ищутся по основе слова:
    // название без двух последних букв, но не короче 4 букв («вестерн» → «весте», подходит «вестерны»).
    private static final int MIN_STEM_LENGTH = 4;

    // Отрицание прямо перед словом («не ужасы», «без мультиков») или через глагол («не хочу ужасы»).
    // Через другие слова отрицание не действует: в «не грустную комедию» комедия нужна.
    private static final Set<String> NEGATIONS = Set.of("не", "без", "кроме");
    private static final Set<String> NEGATED_VERBS = Set.of(
            "хочу", "хочется", "надо", "нужно", "нужен", "нужны", "люблю", "нравится", "нравятся",
            "показывай", "предлагай", "советуй", "смотреть");

    private static final Set<String> GREETING_WORDS = Set.of(
            "привет", "приветик", "приветствую", "здравствуй", "здравствуйте", "здрасте",
            "хай", "хей", "добрый", "доброе", "день", "вечер", "утро", "бот", "hello", "hi");
    private static final Set<String> THANKS_WORDS = Set.of(
            "спасибо", "спасибочки", "спс", "пасиб", "благодарю", "thanks");
    // Слова, которые часто стоят рядом со «спасибо»: «спасибо тебе большое»
    private static final Set<String> THANKS_FILLER = Set.of("тебе", "вам", "большое", "огромное", "бот");

    // «ещё», «покажи другие», «а что-нибудь ещё есть?» — следующие фильмы по прошлому запросу
    private static final Set<String> MORE_WORDS = Set.of("еще", "другие", "другое", "дальше", "больше");
    private static final Set<String> MORE_FILLER = Set.of(
            "а", "и", "давай", "покажи", "покажите", "что", "нибудь", "есть", "можно",
            "варианты", "вариантов", "фильмы", "пожалуйста", "хочу");
    // «а сериал», «лучше мультик», «тогда комедию» — уточнение прошлого запроса (как и слово «теперь»)
    private static final Set<String> REFINE_FIRST_WORDS = Set.of("а", "и", "лучше", "тогда");

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
        return reply(message, null);
    }

    /**
     * @param context что бот помнит о прошлом запросе (null — это первое сообщение)
     */
    public ChatReply reply(String message, ChatContext context) {
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

        // 2. «Ещё» — следующие фильмы по прошлому запросу, модель для этого не нужна
        if (isMoreRequest(words)) {
            if (context == null) {
                return ChatReply.textOnly("Сначала расскажите, что хотите посмотреть. " + EXAMPLES);
            }
            return showMovies(context, new StringBuilder(), true);
        }

        if (!moodPredictor.isAvailable()) {
            return ChatReply.textOnly("Модель для чата не обучена. Выполните команду "
                    + "./mvnw compile exec:java и перезапустите приложение.");
        }

        // 3. Что понял бот. Справочники читаются из базы при каждом сообщении,
        //    поэтому новые жанры и типы из админки сразу понимаются чатом.
        List<Mood> allMoods = moodRepository.findAll();
        List<Genre> allGenres = genreRepository.findAll();
        List<MovieType> allTypes = typeRepository.findAll();
        MoodSuggestion mood = moodPredictor.predict(message)
                .filter(p -> p.probability() >= MOOD_THRESHOLD)
                .orElse(null);
        Mentions<Genre> genres = findInMessage(words, allGenres, Genre::getName);
        Mentions<MovieType> types = findInMessage(words, allTypes, MovieType::getName);

        boolean onlyExclusions = mood == null && genres.wanted() == null && types.wanted() == null;
        if (onlyExclusions && genres.excluded().isEmpty() && types.excluded().isEmpty()) {
            return ChatReply.textOnly("Не совсем понял, что вам хочется посмотреть. Опишите иначе. " + EXAMPLES);
        }

        // 4. Уточнение прошлого запроса («а сериал», «лучше мультик», «только не ужасы»):
        //    новые условия заменяют прежние, остальные остаются. Иначе подбор начинается заново.
        boolean refine = context != null
                && (REFINE_FIRST_WORDS.contains(words.get(0)) || words.contains("теперь") || onlyExclusions);
        ChatContext base = refine ? context : ChatContext.EMPTY;

        // Настроение модели ищем в таблице mood по названию — его могли переименовать или удалить в админке
        Mood moodInDb = mood == null ? null : allMoods.stream()
                .filter(m -> m.getName().equalsIgnoreCase(mood.mood()))
                .findFirst()
                .orElse(null);
        Long moodId = moodInDb != null ? moodInDb.getId() : base.moodId();
        Long genreId = genres.wanted() != null ? genres.wanted().getId() : base.genreId();
        Long typeId = types.wanted() != null ? types.wanted().getId() : base.typeId();

        Set<Long> excludedGenreIds = new HashSet<>(base.excludedGenreIds());
        genres.excluded().forEach(g -> excludedGenreIds.add(g.getId()));
        Set<Long> excludedTypeIds = new HashSet<>(base.excludedTypeIds());
        types.excluded().forEach(t -> excludedTypeIds.add(t.getId()));
        // Новое пожелание отменяет прежний запрет («а теперь ужасы»), а новый запрет — прежнее пожелание
        if (genres.wanted() != null) {
            excludedGenreIds.remove(genreId);
        } else if (excludedGenreIds.contains(genreId)) {
            genreId = null;
        }
        if (types.wanted() != null) {
            excludedTypeIds.remove(typeId);
        } else if (excludedTypeIds.contains(typeId)) {
            typeId = null;
        }

        // id из контекста могли устареть (запись удалили в админке) — такие не учитываются
        Mood moodChosen = findById(allMoods, moodId, Mood::getId);
        Genre genre = findById(allGenres, genreId, Genre::getId);
        MovieType type = findById(allTypes, typeId, MovieType::getId);
        String excluded = Stream.concat(
                        allGenres.stream().filter(g -> excludedGenreIds.contains(g.getId())).map(Genre::getName),
                        allTypes.stream().filter(t -> excludedTypeIds.contains(t.getId())).map(MovieType::getName))
                .collect(Collectors.joining(", "));

        boolean moodMissingInDb = mood != null && moodInDb == null;
        if (moodChosen == null && genre == null && type == null && !moodMissingInDb) {
            return ChatReply.textOnly("Понял, что не хотите: " + excluded
                    + ". А что хочется посмотреть? " + EXAMPLES);
        }

        StringBuilder text = new StringBuilder("Понял так:\n");
        text.append("• настроение: ").append(mood != null ? mood.mood() + " (уверенность " + mood.percent() + "%)"
                : moodChosen != null ? moodChosen.getName() : "не определено").append('\n');
        text.append("• жанр: ").append(genre == null ? "любой" : genre.getName()).append('\n');
        text.append("• тип: ").append(type == null ? "любой" : type.getName()).append('\n');
        if (!excluded.isEmpty()) {
            text.append("• кроме: ").append(excluded).append('\n');
        }

        if (moodMissingInDb) {
            text.append("Настроения «").append(mood.mood()).append("» нет в базе, поэтому его не учитываю.\n");
            if (moodChosen == null && genre == null && type == null) {
                text.append("Уточните жанр или тип. ").append(EXAMPLES);
                return ChatReply.textOnly(text.toString());
            }
        }

        // 5. Подбор существующим сервисом: те же баллы, что и в форме подбора
        return showMovies(new ChatContext(
                moodChosen == null ? null : moodChosen.getId(),
                genre == null ? null : genre.getId(),
                type == null ? null : type.getId(),
                excludedGenreIds, excludedTypeIds, List.of()), text, false);
    }

    /**
     * Следующие MAX_MOVIES фильмов по условиям контекста, которые ещё не показывались.
     * В ответе — новый контекст, где к показанным добавлены эти фильмы.
     */
    private ChatReply showMovies(ChatContext criteria, StringBuilder text, boolean more) {
        List<Movie> remaining = recommendationService.rank(criteria.moodId(), criteria.genreId(),
                        criteria.typeId(), criteria.excludedGenreIds(), criteria.excludedTypeIds()).stream()
                .map(Recommendation::movie)
                .filter(m -> !criteria.shownMovieIds().contains(m.getId()))
                .toList();
        List<Movie> movies = remaining.stream().limit(MAX_MOVIES).toList();

        if (movies.isEmpty()) {
            text.append(more ? "Больше подходящих фильмов нет. Опишите, что ещё хотите посмотреть."
                    : "Подходящих фильмов не нашлось. Попробуйте описать иначе.");
        } else {
            text.append(more ? "Ещё варианты" : "Вот что могу предложить");
            if (remaining.size() > movies.size()) {
                text.append(" (напишите «ещё» — покажу другие)");
            }
            text.append(':');
        }

        List<Long> shown = Stream.concat(criteria.shownMovieIds().stream(), movies.stream().map(Movie::getId))
                .toList();
        ChatContext next = new ChatContext(criteria.moodId(), criteria.genreId(), criteria.typeId(),
                criteria.excludedGenreIds(), criteria.excludedTypeIds(), shown);
        return new ChatReply(text.toString(), movies.stream().map(ChatMovie::from).toList(), next);
    }

    /** Просьба показать ещё фильмы по прошлому запросу: «ещё», «покажи другие», «а что-нибудь ещё есть?». */
    static boolean isMoreRequest(List<String> words) {
        return words.stream().anyMatch(MORE_WORDS::contains)
                && words.stream().allMatch(w -> MORE_WORDS.contains(w) || MORE_FILLER.contains(w));
    }

    private static <T> T findById(List<T> items, Long id, Function<T, Long> idOf) {
        return id == null ? null : items.stream().filter(it -> id.equals(idOf.apply(it))).findFirst().orElse(null);
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

    /** Что нашлось в сообщении: нужная запись (или null) и записи с отрицанием перед ними. */
    record Mentions<T>(T wanted, List<T> excluded) {
    }

    /**
     * Ищет в сообщении записи справочника. Нужная — первая найденная без отрицания перед ней,
     * исключённые — все найденные с отрицанием («только не ужасы», «без мультиков»).
     */
    static <T> Mentions<T> findInMessage(List<String> words, List<T> items, Function<T, String> nameOf) {
        T wanted = null;
        List<T> excluded = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            T item = items.stream()
                    .filter(it -> matches(word, normalize(nameOf.apply(it))))
                    .findFirst()
                    .orElse(null);
            if (item == null) {
                continue;
            }
            if (isNegated(words, i)) {
                if (!excluded.contains(item)) {
                    excluded.add(item);
                }
            } else if (wanted == null) {
                wanted = item;
            }
        }
        // «ужасы? нет, только не ужасы» — отрицание важнее
        if (excluded.contains(wanted)) {
            wanted = null;
        }
        return new Mentions<>(wanted, excluded);
    }

    /** Подходит ли слово к названию: по словам из KEYWORDS, а если названия там нет — по основе. */
    static boolean matches(String word, String name) {
        return KEYWORDS.getOrDefault(name, List.of(stem(name))).stream().anyMatch(word::startsWith);
    }

    /** Стоит ли перед словом отрицание: «не ужасы» или «не хочу ужасы». */
    static boolean isNegated(List<String> words, int index) {
        if (index >= 1 && NEGATIONS.contains(words.get(index - 1))) {
            return true;
        }
        return index >= 2 && NEGATED_VERBS.contains(words.get(index - 1))
                && NEGATIONS.contains(words.get(index - 2));
    }

    static String stem(String name) {
        int length = Math.max(MIN_STEM_LENGTH, name.length() - 2);
        return name.substring(0, Math.min(length, name.length()));
    }

    private static String normalize(String text) {
        return text.toLowerCase().replace('ё', 'е');
    }
}
