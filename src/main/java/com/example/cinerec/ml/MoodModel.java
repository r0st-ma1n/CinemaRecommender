package com.example.cinerec.ml;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.tokenizers.CharacterNGramTokenizer;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Общие настройки модели настроений. Используются и при обучении (TrainMoodModel),
 * и при предсказании в приложении (MoodPredictor), чтобы данные готовились одинаково.
 * По этим же настройкам обучается модель для чата (TrainChatModel).
 *
 * Как устроена модель:
 * 1. Текст = описание фильма + его жанры.
 * 2. StringToWordVector превращает текст в числа: сколько раз в нём встречается
 *    каждый кусочек из 3–5 букв (символьные n-граммы). Кусочки вместо целых слов
 *    нужны из-за русских окончаний: «войны», «войне», «войной» дают общее «войн».
 * 3. Классификатор SMO (метод опорных векторов) по этим числам выбирает настроение.
 *    Включённая калибровка (buildCalibrationModels) даёт ещё и вероятность каждого
 *    настроения — по ним админке предлагаются одно-два самых вероятных.
 *
 * Почему так: при сравнении (перекрёстная проверка по 10 частям) этот вариант оказался
 * точнее, чем целые слова вместо кусочков и чем наивный байесовский классификатор.
 */
public final class MoodModel {

    static final String TEXT_ATTRIBUTE = "text";
    static final String MOOD_ATTRIBUTE = "mood";

    private MoodModel() {
    }

    /** Текст, по которому модель угадывает настроение. */
    public static String toText(String description, Collection<String> genres) {
        return description + " Жанры: " + String.join(", ", genres);
    }

    /** Пустой набор данных: текстовый атрибут и класс-настроение с заданными значениями. */
    public static Instances emptyDataset(List<String> moods) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(TEXT_ATTRIBUTE, (List<String>) null)); // null = строковый атрибут
        attributes.add(new Attribute(MOOD_ATTRIBUTE, moods));
        Instances data = new Instances("movie-moods", attributes, 0);
        data.setClassIndex(1);
        return data;
    }

    /** Одна строка данных; mood == null, если настроение неизвестно (при предсказании). */
    public static Instance newInstance(Instances dataset, String text, String mood) {
        Instance instance = new DenseInstance(2);
        instance.setDataset(dataset);
        instance.setValue(0, text);
        if (mood == null) {
            instance.setClassMissing();
        } else {
            instance.setClassValue(mood);
        }
        return instance;
    }

    /** Новый необученный классификатор: фильтр «текст → числа» + SMO. */
    public static Classifier newClassifier() {
        CharacterNGramTokenizer tokenizer = new CharacterNGramTokenizer();
        tokenizer.setNGramMinSize(3);
        tokenizer.setNGramMaxSize(5);

        StringToWordVector toVector = new StringToWordVector();
        toVector.setTokenizer(tokenizer);
        toVector.setLowerCaseTokens(true);
        toVector.setOutputWordCounts(true);   // считать, сколько раз встречается кусочек
        toVector.setWordsToKeep(5000);

        SMO smo = new SMO();
        smo.setBuildCalibrationModels(true);  // чтобы получать вероятности настроений
        // Вероятности настраиваются на внутренней проверке по 5 частям, а не на тех же
        // примерах, на которых училась модель, — иначе она слишком уверена в себе (99–100%)
        smo.setNumFolds(5);

        // FilteredClassifier сохраняет фильтр вместе с моделью,
        // поэтому новый текст при предсказании обрабатывается так же, как при обучении
        FilteredClassifier classifier = new FilteredClassifier();
        classifier.setFilter(toVector);
        classifier.setClassifier(smo);
        return classifier;
    }
}
