package com.example.cinerec.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Годы выпуска, о которых говорится в сообщении: «из 90-х», «после 2010», «до 2000», «новое», «старое».
 * Обе границы включительные, любая может быть null («не раньше 2011»).
 */
record YearRange(Integer from, Integer to) {

    // «Новое» — последние 10 лет каталога, «старое» — на 30 лет и более старше самого нового фильма.
    // Отсчёт от самого нового фильма в базе, а не от текущего года: иначе при каталоге,
    // который заканчивается 2020 годом, «новых» фильмов не нашлось бы совсем.
    static final int NEW_YEARS = 10;
    static final int OLD_YEARS = 30;

    // Десятилетия цифрами: «90-х», «90х», «90-е», «1990-х», «2000-х», «00-х»
    private static final Pattern DECADE = Pattern.compile("(?<!\\d)((?:19|20)\\d|\\d)0\\s*-?\\s*(?:х|ых|е|ые)(?!\\p{L})");
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)((?:19|20)\\d\\d)(?!\\d)");

    // Десятилетия словами — по началу слова: «девяностых», «нулевые»
    private static final Map<String, Integer> DECADE_WORDS = Map.of(
            "шестидесят", 1960, "семидесят", 1970, "восьмидесят", 1980, "девяност", 1990,
            "нулев", 2000, "двухтысячн", 2000, "десятых", 2010, "десятые", 2010);

    // Слово перед годом: «после 2010» — с 2011, «с 2010» — с 2010, «до 2000» — по 1999, «по 1999» — по 1999
    private static final Set<String> AFTER = Set.of("после", "позже", "позднее", "новее");
    private static final Set<String> SINCE = Set.of("с", "со", "от");
    private static final Set<String> BEFORE = Set.of("до", "раньше", "ранее", "старше");
    private static final Set<String> UNTIL = Set.of("по");

    private static final Set<String> NEW_WORDS = Set.of(
            "новое", "новые", "новый", "новая", "новую", "новенькое", "новенькие", "новенький",
            "новинки", "новинку", "новинка", "новинок", "свежее", "свежие", "свежий", "свежую",
            "недавнее", "недавние", "недавний", "недавнюю",
            "современное", "современные", "современный", "современную");
    private static final Set<String> OLD_WORDS = Set.of(
            "старое", "старые", "старый", "старую", "старенькое", "старенькие", "старенький", "старенькую",
            "давнее", "давние");

    /**
     * Годы из сообщения или null, если о годах ничего не сказано.
     * Если условий несколько («с 1990 по 1999», «новую комедию после 2015»), они все действуют сразу.
     *
     * @param words      слова сообщения (см. ChatService.words)
     * @param newestYear год самого нового фильма в базе — запрашивается, только если он нужен
     */
    static YearRange parse(String message, List<String> words, Supplier<Integer> newestYear) {
        String text = message.toLowerCase();
        List<YearRange> parts = new ArrayList<>();

        Matcher decade = DECADE.matcher(text);
        while (decade.find()) {
            String digits = decade.group(1);
            int start = digits.length() == 1
                    ? (digits.charAt(0) >= '3' ? 1900 : 2000) + (digits.charAt(0) - '0') * 10 // «90-х», «00-х»
                    : Integer.parseInt(digits) * 10;                                         // «1990-х»
            parts.add(decade(start));
        }

        // Отдельные годы ищем в тексте без десятилетий, чтобы «2000-х» не считалось ещё и 2000 годом
        String rest = DECADE.matcher(text).replaceAll(" ");
        Integer exactFrom = null, exactTo = null;
        Matcher year = YEAR.matcher(rest);
        while (year.find()) {
            int y = Integer.parseInt(year.group(1));
            String before = lastWord(rest.substring(0, year.start()));
            if (AFTER.contains(before)) {
                parts.add(new YearRange(y + 1, null));
            } else if (SINCE.contains(before)) {
                parts.add(new YearRange(y, null));
            } else if (BEFORE.contains(before)) {
                parts.add(new YearRange(null, y - 1));
            } else if (UNTIL.contains(before)) {
                parts.add(new YearRange(null, y));
            } else { // «фильм 2010 года»; несколько годов подряд — от первого до последнего
                exactFrom = exactFrom == null ? y : Math.min(exactFrom, y);
                exactTo = exactTo == null ? y : Math.max(exactTo, y);
            }
        }
        if (exactFrom != null) {
            parts.add(new YearRange(exactFrom, exactTo));
        }

        boolean needsNewest = words.stream().anyMatch(w -> NEW_WORDS.contains(w) || OLD_WORDS.contains(w));
        Integer newest = needsNewest ? newestYear.get() : null;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            DECADE_WORDS.forEach((prefix, start) -> {
                if (word.startsWith(prefix)) {
                    parts.add(decade(start));
                }
            });
            // «на новый год» — это праздник, а не годы выпуска
            boolean isNew = NEW_WORDS.contains(word) && !(i + 1 < words.size() && words.get(i + 1).startsWith("год"));
            if (newest != null && (isNew || OLD_WORDS.contains(word))) {
                boolean wantNew = isNew != ChatService.isNegated(words, i); // «не старое» — новое
                parts.add(wantNew ? new YearRange(newest - NEW_YEARS + 1, null)
                        : new YearRange(null, newest - OLD_YEARS));
            }
        }

        return parts.stream().reduce(YearRange::intersect).orElse(null);
    }

    /** «1990–1999», «2010 год», «не раньше 2011», «не позже 1990». */
    String describe() {
        if (from != null && from.equals(to)) {
            return from + " год";
        }
        if (from != null && to != null) {
            return from + "–" + to;
        }
        return from != null ? "не раньше " + from : "не позже " + to;
    }

    private YearRange intersect(YearRange other) {
        return new YearRange(
                from == null ? other.from : other.from == null ? from : Math.max(from, other.from),
                to == null ? other.to : other.to == null ? to : Math.min(to, other.to));
    }

    private static YearRange decade(int start) {
        return new YearRange(start, start + 9);
    }

    private static String lastWord(String text) {
        String[] words = text.split("[^\\p{L}]+");
        return Arrays.stream(words).filter(w -> !w.isEmpty()).reduce((a, b) -> b).orElse("");
    }
}
