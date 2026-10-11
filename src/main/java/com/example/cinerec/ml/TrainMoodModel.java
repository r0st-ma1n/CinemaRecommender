package com.example.cinerec.ml;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Обучение модели подсказок для админки (настроение по описанию фильма).
 * Запускается из TrainModels вместе с моделью для чата: ./mvnw compile exec:java
 *
 * 1. Читает обучающий набор ml/training-data.csv (название;настроение;жанры;описание).
 * 2. Делит его на обучающую (80%) и тестовую (20%) части и выводит точность на тестовой.
 * 3. Обучает итоговую модель на всех данных и сохраняет её в ml/mood-model.model.
 */
public class TrainMoodModel {

    // Настроения в том же порядке, что и в базе
    static final List<String> MOODS =
            List.of("весёлое", "уютное", "напряжённое", "грустное", "вдохновляющее", "мрачное");

    private static final int SEED = 42; // фиксированное «зерно» — результат одинаков при каждом запуске

    static void train(Path csv, Path modelFile) throws Exception {
        Instances data = readCsv(csv);
        System.out.println("Загружено фильмов: " + data.numInstances());

        // Перемешиваем и делим на 5 частей с равной долей каждого настроения:
        // одна часть (20%) — тестовая, остальные (80%) — обучающая.
        data.randomize(new Random(SEED));
        data.stratify(5);
        Instances train = data.trainCV(5, 0);
        Instances test = data.testCV(5, 0);
        System.out.println("Обучающая часть: " + train.numInstances() + ", тестовая: " + test.numInstances());

        Classifier classifier = MoodModel.newClassifier();
        classifier.buildClassifier(train);

        Evaluation evaluation = new Evaluation(train);
        evaluation.evaluateModel(classifier, test);
        System.out.printf("%nТочность на тестовой части: %.1f%%%n", evaluation.pctCorrect());
        System.out.printf("Верное настроение среди двух самых вероятных: %.1f%%%n",
                topTwoAccuracy(classifier, test));
        System.out.println(evaluation.toMatrixString("\nМатрица ошибок (строки — правильный ответ, столбцы — ответ модели):"));

        // Итоговая модель учится на всех 100% данных — так она знает больше примеров.
        Classifier finalModel = MoodModel.newClassifier();
        finalModel.buildClassifier(data);
        Files.createDirectories(modelFile.toAbsolutePath().getParent());
        // Вместе с моделью сохраняем пустой набор данных — «шапку» с описанием атрибутов,
        // по ней приложение создаёт строку для предсказания.
        SerializationHelper.writeAll(modelFile.toString(), new Object[]{finalModel, new Instances(data, 0)});
        System.out.println("Модель сохранена: " + modelFile.toAbsolutePath());
    }

    /** Чтение CSV с разделителем «;». Первая строка — заголовок. */
    static Instances readCsv(Path csv) throws IOException {
        Instances data = MoodModel.emptyDataset(MOODS);
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(";", 4);
            String mood = parts[1];
            List<String> genres = Arrays.asList(parts[2].split(","));
            String description = parts[3];
            data.add(MoodModel.newInstance(data, MoodModel.toText(description, genres), mood));
        }
        return data;
    }

    /** Доля фильмов, у которых правильное настроение попало в два самых вероятных ответа модели. */
    private static double topTwoAccuracy(Classifier classifier, Instances test) throws Exception {
        int hits = 0;
        for (Instance instance : test) {
            double[] probabilities = classifier.distributionForInstance(instance);
            int correct = (int) instance.classValue();
            int betterThanCorrect = 0;
            for (double p : probabilities) {
                if (p > probabilities[correct]) {
                    betterThanCorrect++;
                }
            }
            if (betterThanCorrect < 2) {
                hits++;
            }
        }
        return 100.0 * hits / test.numInstances();
    }
}
