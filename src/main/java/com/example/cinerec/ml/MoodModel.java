package com.example.cinerec.ml;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.tokenizers.CharacterNGramTokenizer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
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
 * точнее, чем целые слова вместо кусочков, чем наивный байесовский классификатор
 * и чем нейросеть — многослойный перцептрон (newNeuralNetwork, сравнение — CompareChatModels).
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
        SMO smo = new SMO();
        smo.setBuildCalibrationModels(true);  // чтобы получать вероятности настроений
        // Вероятности настраиваются на внутренней проверке по 5 частям, а не на тех же
        // примерах, на которых училась модель, — иначе она слишком уверена в себе (99–100%)
        smo.setNumFolds(5);

        // FilteredClassifier сохраняет фильтр вместе с моделью,
        // поэтому новый текст при предсказании обрабатывается так же, как при обучении
        FilteredClassifier classifier = new FilteredClassifier();
        classifier.setFilter(newTextFilter());
        classifier.setClassifier(smo);
        return classifier;
    }

    /**
     * Нейросеть для сравнения с SMO (CompareChatModels): многослойный перцептрон
     * с одним скрытым слоем из 32 нейронов, обучение обратным распространением ошибки.
     * В приложении не используется — на фразах для чата она точнее SMO не оказалась.
     *
     * Настройки подобраны перекрёстной проверкой:
     * - перцептрон Weka не умеет работать с разреженными данными и на всех ~16 тысячах
     *   кусочков учится часами, поэтому оставляется 1000 самых информативных
     *   (по информационному выигрышу). Отбор — внутри FilteredClassifier, то есть
     *   только по обучающим данным, без подглядывания в проверочные;
     * - нормировка входов отключена: по умолчанию Weka переводит каждый вход в [-1; 1],
     *   и отсутствующий кусочек (0) становится -1. Тогда «включены» сразу все входы,
     *   нейроны скрытого слоя насыщаются, и точность падает до 17–39%;
     * - скорость обучения 0,05 вместо 0,3 по умолчанию — при большей сеть хуже сходится.
     */
    public static Classifier newNeuralNetwork() {
        AttributeSelection select = new AttributeSelection();
        select.setEvaluator(new InfoGainAttributeEval());
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(1000);
        select.setSearch(ranker);

        MultiFilter filters = new MultiFilter();
        filters.setFilters(new Filter[]{newTextFilter(), select});

        MultilayerPerceptron mlp = new MultilayerPerceptron();
        mlp.setHiddenLayers("32");
        mlp.setLearningRate(0.05);
        mlp.setMomentum(0.2);
        mlp.setTrainingTime(300);              // эпох обучения
        mlp.setNormalizeAttributes(false);
        mlp.setSeed(42);

        FilteredClassifier classifier = new FilteredClassifier();
        classifier.setFilter(filters);
        classifier.setClassifier(mlp);
        return classifier;
    }

    /** Фильтр «текст → числа»: сколько раз встречается каждый кусочек из 3–5 букв. */
    private static StringToWordVector newTextFilter() {
        CharacterNGramTokenizer tokenizer = new CharacterNGramTokenizer();
        tokenizer.setNGramMinSize(3);
        tokenizer.setNGramMaxSize(5);

        StringToWordVector toVector = new StringToWordVector();
        toVector.setTokenizer(tokenizer);
        toVector.setLowerCaseTokens(true);
        toVector.setOutputWordCounts(true);   // считать, сколько раз встречается кусочек
        toVector.setWordsToKeep(5000);
        return toVector;
    }
}
