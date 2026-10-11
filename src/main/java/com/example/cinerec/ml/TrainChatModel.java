package com.example.cinerec.ml;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Обучение модели для чата: настроение по фразе пользователя («хочу поржать» → весёлое).
 * Устроена так же, как модель подсказок (MoodModel: символьные n-граммы + SMO),
 * но учится на коротких разговорных фразах, а не на описаниях сюжетов.
 * Запускается из TrainModels: ./mvnw compile exec:java
 *
 * 1. Читает ml/chat-training-data.csv (настроение;фраза). Список настроений берётся из файла.
 * 2. Оценивает точность перекрёстной проверкой на 10 частях: модель 10 раз учится на 9/10 фраз
 *    и проверяется на оставшейся 1/10, так что каждая фраза один раз побывает в проверке.
 * 3. Обучает итоговую модель на всех фразах и сохраняет её в ml/chat-model.model.
 */
public class TrainChatModel {

    private static final int SEED = 42;
    private static final int FOLDS = 10;

    static void train(Path csv, Path modelFile) throws Exception {
        Instances data = readCsv(csv);
        System.out.println("Загружено фраз: " + data.numInstances()
                + ", настроения: " + listMoods(data));

        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(MoodModel.newClassifier(), data, FOLDS, new Random(SEED));
        System.out.printf("%nТочность (перекрёстная проверка, %d частей): %.1f%%%n",
                FOLDS, evaluation.pctCorrect());
        System.out.println(evaluation.toMatrixString(
                "\nМатрица ошибок (строки — правильный ответ, столбцы — ответ модели):"));

        Classifier finalModel = MoodModel.newClassifier();
        finalModel.buildClassifier(data);
        Files.createDirectories(modelFile.toAbsolutePath().getParent());
        SerializationHelper.writeAll(modelFile.toString(), new Object[]{finalModel, new Instances(data, 0)});
        System.out.println("Модель сохранена: " + modelFile.toAbsolutePath());
    }

    /** Чтение CSV «настроение;фраза». Первая строка — заголовок. */
    static Instances readCsv(Path csv) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(csv, StandardCharsets.UTF_8).stream().skip(1).toList()) {
            if (!line.isBlank()) {
                rows.add(line.split(";", 2));
            }
        }

        // Настроения — все разные значения первого столбца в порядке появления в файле
        Set<String> moods = new LinkedHashSet<>();
        for (String[] row : rows) {
            moods.add(row[0]);
        }

        Instances data = MoodModel.emptyDataset(new ArrayList<>(moods));
        for (String[] row : rows) {
            data.add(MoodModel.newInstance(data, row[1], row[0]));
        }
        return data;
    }

    private static List<String> listMoods(Instances data) {
        List<String> moods = new ArrayList<>();
        for (int i = 0; i < data.classAttribute().numValues(); i++) {
            moods.add(data.classAttribute().value(i));
        }
        return moods;
    }
}
