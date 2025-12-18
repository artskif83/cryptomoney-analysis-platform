package artskif.trader.contract.features;

import artskif.trader.dto.CandlestickDto;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Интерфейс для создателей фич (features)
 * Каждый индикатор должен реализовывать этот интерфейс для добавления фич в контракт
 */
public interface Feature {


    /**
     * Получить список свечей для расчета фичи
     *
     * @return список свечей
     */
    List<CandlestickDto> getCandlestickDtos();

    /**
     * Получить индикатор TA4J для расчета фичи
     *
     * @return индикатор TA4J
     */
    AbstractIndicator<Num> getIndicator();

    /**
     * Получить имя фичи
     *
     * @return имя фичи в БД
     */
    String getFeatureName();

    /**
     * Получить тип данных фичи для создания колонки в БД
     *
     * @return SQL тип данных
     */
    String getDataType();
}

