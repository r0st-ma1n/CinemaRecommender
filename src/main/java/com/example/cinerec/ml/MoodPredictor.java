package com.example.cinerec.ml;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Загружает обученную модель при старте приложения и предсказывает настроение фильма
 * по описанию и жанрам. Если файла модели нет, приложение всё равно работает,
 * просто без подсказок (в логе будет предупреждение).
 */
@Service
public class MoodPredictor {

    private static final Logger log = LoggerFactory.getLogger(MoodPredictor.class);

    // Второе настроение предлагается, только если оно не сильно уступает первому:
    // его вероятность должна быть не меньше половины вероятности первого.
    private static final double SECOND_MOOD_RATIO = 0.5;

    private final Path modelPath;
    private Classifier classifier;
    private Instances header;

    public MoodPredictor(@Value("${cinerec.model.path}") String modelPath) {
        this.modelPath = Path.of(modelPath);
    }

    @PostConstruct
    void loadModel() {
        if (!Files.exists(modelPath)) {
            log.warn("Файл модели {} не найден — подсказки настроения отключены. "
                    + "Обучите модель: ./mvnw compile exec:java", modelPath.toAbsolutePath());
            return;
        }
        try {
            Object[] saved = SerializationHelper.readAll(modelPath.toString());
            classifier = (Classifier) saved[0];
            header = (Instances) saved[1];
            log.info("Модель настроений загружена из {}", modelPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Не удалось загрузить модель {} — подсказки отключены", modelPath.toAbsolutePath(), e);
        }
    }

    public boolean isAvailable() {
        return classifier != null;
    }

    /**
     * Одно-два самых вероятных настроения для фильма.
     * synchronized: фильтр внутри модели при обработке текста меняет своё состояние,
     * поэтому одновременно её использует только один запрос.
     */
    public synchronized List<MoodSuggestion> suggest(String description, Collection<String> genres) {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            String text = MoodModel.toText(description, genres);
            double[] probabilities = classifier.distributionForInstance(MoodModel.newInstance(header, text, null));

            List<MoodSuggestion> all = new ArrayList<>();
            for (int i = 0; i < probabilities.length; i++) {
                all.add(new MoodSuggestion(header.classAttribute().value(i), probabilities[i]));
            }
            all.sort(Comparator.comparingDouble(MoodSuggestion::probability).reversed());

            List<MoodSuggestion> result = new ArrayList<>();
            result.add(all.get(0));
            if (all.get(1).probability() >= all.get(0).probability() * SECOND_MOOD_RATIO) {
                result.add(all.get(1));
            }
            return result;
        } catch (Exception e) {
            log.error("Ошибка предсказания настроения", e);
            return List.of();
        }
    }
}
