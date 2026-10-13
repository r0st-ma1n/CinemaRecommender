package com.example.cinerec.ml;

import java.nio.file.Path;

/**
 * Отдельная программа (не часть веб-приложения), которая обучает обе модели одной командой:
 * ./mvnw compile exec:java
 *
 * 1. Модель подсказок для админки: настроение по описанию фильма.
 * 2. Модель для чата: настроение по фразе пользователя.
 *
 * С аргументом compare (./mvnw compile exec:java -Dexec.args=compare) модели не обучаются,
 * а модель для чата сравнивается с нейросетью — см. CompareChatModels.
 */
public class TrainModels {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("compare")) {
            System.out.println("=== Сравнение моделей для чата: SMO и нейросеть ===");
            CompareChatModels.compare(Path.of("ml/chat-training-data.csv"));
            return;
        }
        System.out.println("=== Модель подсказок для админки (по описаниям фильмов) ===");
        TrainMoodModel.train(Path.of("ml/training-data.csv"), Path.of("ml/mood-model.model"));

        System.out.println("\n=== Модель для чата (по фразам пользователей) ===");
        TrainChatModel.train(Path.of("ml/chat-training-data.csv"), Path.of("ml/chat-model.model"));
    }
}
