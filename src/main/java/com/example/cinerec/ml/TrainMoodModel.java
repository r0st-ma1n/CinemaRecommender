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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Обучение модели подсказок для админки (настроение по описанию фильма).
 * Запускается из TrainModels вместе с моделью для чата: ./mvnw compile exec:java
 *
 * 1. Читает обучающий набор ml/training-data.csv (название;настроение;жанры;описание).
 *    Список настроений берётся из файла.
 * 2. Делит его на обучающую (80%) и тестовую (20%) части и выводит точность на тестовой.
 * 3. Выводит точность по перекрёстной проверке на 10 частях — она надёжнее, чем одна
 *    тестовая часть из ~50 фильмов, потому что в проверке побывает каждый фильм.
 * 4. Обучает итоговую модель на всех данных и сохраняет её в ml/mood-model.model.
 */
public class TrainMoodModel {

    private static final int SEED = 42; // фиксированное «зерно» — результат одинаков при каждом запуске
    private static final int FOLDS = 10;

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

        Evaluation crossValidation = new Evaluation(data);
        crossValidation.crossValidateModel(MoodModel.newClassifier(), data, FOLDS, new Random(SEED));
        System.out.printf("Точность (перекрёстная проверка, %d частей): %.1f%%%n%n",
                FOLDS, crossValidation.pctCorrect());

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
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(csv, StandardCharsets.UTF_8).stream().skip(1).toList()) {
            if (!line.isBlank()) {
                rows.add(line.split(";", 4)); // название;настроение;жанры;описание
            }
        }

        // Настроения — все разные значения второго столбца в порядке появления в файле
        Set<String> moods = new LinkedHashSet<>();
        for (String[] row : rows) {
            moods.add(row[1]);
        }

        Instances data = MoodModel.emptyDataset(new ArrayList<>(moods));
        for (String[] row : rows) {
            List<String> genres = Arrays.asList(row[2].split(","));
            data.add(MoodModel.newInstance(data, MoodModel.toText(row[3], genres), row[1]));
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
