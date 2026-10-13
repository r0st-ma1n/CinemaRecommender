package com.example.cinerec.ml;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Сравнение модели для чата (SMO) с нейросетью — многослойным перцептроном.
 * Запуск: ./mvnw compile exec:java -Dexec.args=compare (около 10 минут, модели не сохраняются)
 *
 * Обе модели проверяются перекрёстной проверкой на 10 частях на одних и тех же частях фраз.
 * Для каждой выводятся точность, доля «верное настроение среди двух самых вероятных»,
 * таблица порогов уверенности (как в TrainChatModel) и время проверки.
 * Части проверяются параллельно: перцептрон учится в сотни раз дольше SMO.
 */
public class CompareChatModels {

    private static final int SEED = 42;
    private static final int FOLDS = 10;
    private static final double[] THRESHOLDS = {0.3, 0.4, 0.5, 0.6, 0.7};

    static void compare(Path csv) throws Exception {
        Instances data = TrainChatModel.readCsv(csv);
        System.out.println("Загружено фраз: " + data.numInstances());
        evaluate("SMO (метод опорных векторов)", MoodModel::newClassifier, data);
        evaluate("Многослойный перцептрон (нейросеть)", MoodModel::newNeuralNetwork, data);
    }

    private static void evaluate(String name, Supplier<Classifier> newModel, Instances data) throws Exception {
        long start = System.currentTimeMillis();

        // Деление на части — как в Evaluation.crossValidateModel: перемешать, разложить
        // настроения по частям поровну, каждая часть один раз становится проверочной
        Instances shuffled = new Instances(data);
        Random random = new Random(SEED);
        shuffled.randomize(random);
        shuffled.stratify(FOLDS);
        List<Instances[]> folds = new ArrayList<>();
        for (int i = 0; i < FOLDS; i++) {
            folds.add(new Instances[]{shuffled.trainCV(FOLDS, i, random), shuffled.testCV(FOLDS, i)});
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(FOLDS, Runtime.getRuntime().availableProcessors()));
        List<Future<List<double[]>>> results = new ArrayList<>();
        for (Instances[] fold : folds) {
            results.add(pool.submit(() -> {
                Classifier classifier = newModel.get();
                classifier.buildClassifier(fold[0]);
                List<double[]> predictions = new ArrayList<>();
                for (Instance instance : fold[1]) {
                    double[] p = classifier.distributionForInstance(instance);
                    double[] withActual = Arrays.copyOf(p, p.length + 1);
                    withActual[p.length] = instance.classValue(); // последним — правильный ответ
                    predictions.add(withActual);
                }
                return predictions;
            }));
        }
        List<double[]> predictions = new ArrayList<>();
        for (Future<List<double[]>> result : results) {
            predictions.addAll(result.get());
        }
        pool.shutdown();

        int correct = 0;
        int topTwo = 0;
        int[] determined = new int[THRESHOLDS.length];
        int[] determinedCorrect = new int[THRESHOLDS.length];
        for (double[] row : predictions) {
            int actual = (int) row[row.length - 1];
            int best = 0;
            int betterThanActual = 0;
            for (int i = 0; i < row.length - 1; i++) {
                if (row[i] > row[best]) {
                    best = i;
                }
                if (row[i] > row[actual]) {
                    betterThanActual++;
                }
            }
            if (best == actual) {
                correct++;
            }
            if (betterThanActual < 2) {
                topTwo++;
            }
            for (int t = 0; t < THRESHOLDS.length; t++) {
                if (row[best] >= THRESHOLDS[t]) {
                    determined[t]++;
                    if (best == actual) {
                        determinedCorrect[t]++;
                    }
                }
            }
        }

        int total = predictions.size();
        System.out.printf("%n--- %s ---%n", name);
        System.out.printf("Точность (перекрёстная проверка, %d частей): %.1f%%%n", FOLDS, 100.0 * correct / total);
        System.out.printf("Верное настроение среди двух самых вероятных: %.1f%%%n", 100.0 * topTwo / total);
        System.out.println("Порог | настроение определено | из них верно");
        for (int t = 0; t < THRESHOLDS.length; t++) {
            System.out.printf("  %.1f |   %5.1f%% фраз            |  %5.1f%%%n", THRESHOLDS[t],
                    100.0 * determined[t] / total,
                    determined[t] == 0 ? 0 : 100.0 * determinedCorrect[t] / determined[t]);
        }
        System.out.printf("Время проверки: %.0f с%n", (System.currentTimeMillis() - start) / 1000.0);
    }
}
