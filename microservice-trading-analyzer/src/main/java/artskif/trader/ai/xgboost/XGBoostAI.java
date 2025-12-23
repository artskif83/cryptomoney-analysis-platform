package artskif.trader.ai.xgboost;

import artskif.trader.ai.AbstractAI;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.jboss.logging.Logger;
//import ml.dmlc.xgboost4j.java.*;
import java.util.*;

import java.util.List;

@Startup
@ApplicationScoped
public class XGBoostAI extends AbstractAI {

    private final static Logger LOG = Logger.getLogger(XGBoostAI.class);

    @PostConstruct
    void start() throws XGBoostError {
//        LOG.infof("🚀 Старт искуственного интелекта %s", getName());
//
//        // ===== 1) Пример обучающих данных =====
//        float[] data = {
//                50f,  10f,  0.003f,   // sample 1: RSI, Volume_delta, EMA_slope
//                55f,  12f,  0.004f,   // sample 2
//                60f,  -5f,  0.006f,   // sample 3
//                48f,  -2f,  0.002f    // sample 4
//        };
//
//        float[] labels = {25000f, 25500f, 26000f, 25200f};
//
//        // 4 строки, 3 признака
//        DMatrix trainData = new DMatrix(data, 4, 3, Float.NaN);
//        trainData.setLabel(labels);
//
//        // ===== 2) Параметры XGBoost для уменьшения переобучения =====
//        Map<String, Object> params = new HashMap<>();
//
//        // --- Контроль сложности деревьев ---
//        params.put("max_depth", 4);          // чем меньше, тем меньше переобучение
//        params.put("min_child_weight", 6);   // деревья растут только при достаточном числе данных
//
//        // --- Регуляризация ветвей ---
//        params.put("lambda", 4.0);           // L2-регуляризация (стабилизирует модель)
//        params.put("alpha", 0.2);            // L1-регуляризация (убирает слабые признаки)
//
//        // --- Контроль шага обучения ---
//        params.put("eta", 0.07);             // небольшой шаг → модель учится плавнее
//
//        // --- Сэмплирование для борьбы с переобучением ---
//        params.put("subsample", 0.75);       // случайная часть строк для каждого дерева
//        params.put("colsample_bytree", 0.6); // случайная часть признаков для каждого дерева
//
//        // --- Тип задачи ---
//        params.put("objective", "reg:squarederror");
//        params.put("eval_metric", "rmse");
//
//        int numRounds = 150;  // можно больше, т.к. eta маленькая
//
//        // ===== 3) Обучение =====
//        Booster booster = XGBoost.train(trainData, params, numRounds, new HashMap<>(), null, null);
//
//        // ===== 4) Тестовый пример =====
//        float[] test = {57f, 4.0f, 0.005f};  // RSI, Volume_delta, EMA_slope
//        DMatrix testData = new DMatrix(test, 1, 3, Float.NaN);
//
//        float[][] prediction = booster.predict(testData);
//        System.out.println("Prediction: " + prediction[0][0]);
//
//        // ===== 5) Вклады признаков (как SHAP-contribs) =====
//        float[][] contribs = booster.predict(
//                testData,
//                false,
//                0,
//                Booster.PredictorType.PREDICT_CONTRIB
//        );
//        //float[][] contribs = booster.predict(testData, true);
//
//        System.out.println("\nFeature contributions:");
//        for (int i = 0; i < contribs[0].length; i++) {
//            System.out.println("Feature " + i + ": " + contribs[0][i]);
//        }
//
//        // ===== 6) Сохранение модели =====
//        booster.saveModel("model_overfit_safe.xgb");
    }

    @PreDestroy
    void stop() {

    }

    @Override
    protected String getName() {
        return "XGBoost AI";
    }
}
