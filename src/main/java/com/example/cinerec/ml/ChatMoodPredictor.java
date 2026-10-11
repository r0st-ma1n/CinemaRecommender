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
import java.util.Optional;

/**
 * Модель для чата: загружается при старте приложения и угадывает настроение по фразе пользователя.
 * Если файла модели нет, isAvailable() возвращает false и чат сообщает, что модель не обучена.
 */
@Service
public class ChatMoodPredictor {

    private static final Logger log = LoggerFactory.getLogger(ChatMoodPredictor.class);

    private final Path modelPath;
    private Classifier classifier;
    private Instances header;

    public ChatMoodPredictor(@Value("${cinerec.chat-model.path}") String modelPath) {
        this.modelPath = Path.of(modelPath);
    }

    @PostConstruct
    void loadModel() {
        if (!Files.exists(modelPath)) {
            log.warn("Файл модели чата {} не найден — чат не сможет определять настроение. "
                    + "Обучите модели: ./mvnw compile exec:java", modelPath.toAbsolutePath());
            return;
        }
        try {
            Object[] saved = SerializationHelper.readAll(modelPath.toString());
            classifier = (Classifier) saved[0];
            header = (Instances) saved[1];
            log.info("Модель чата загружена из {}", modelPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Не удалось загрузить модель чата {}", modelPath.toAbsolutePath(), e);
        }
    }

    public boolean isAvailable() {
        return classifier != null;
    }

    /**
     * Самое вероятное настроение для фразы и его вероятность.
     * synchronized — по той же причине, что и в MoodPredictor: фильтр внутри модели меняет своё состояние.
     */
    public synchronized Optional<MoodSuggestion> predict(String message) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            double[] probabilities = classifier.distributionForInstance(
                    MoodModel.newInstance(header, message, null));
            int best = 0;
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > probabilities[best]) {
                    best = i;
                }
            }
            return Optional.of(new MoodSuggestion(header.classAttribute().value(best), probabilities[best]));
        } catch (Exception e) {
            log.error("Ошибка предсказания настроения в чате", e);
            return Optional.empty();
        }
    }
}
