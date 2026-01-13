package artskif.trader.strategy.contract.labels;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Интерфейс для генераторов лейблов (целевых переменных для ML)
 */
public interface Label {
    /**
     * Имя лейбла (имя колонки в БД).
     */
    String getLabelName();

    /**
     * SQL-тип данных для колонки лейбла.
     */
    String getDataType();

    /**
     * Список свечей, на основе которых считаем лейбл.
     * Обычно тот же набор, что и для фич.
     */
    List<CandlestickDto> getCandlestickDtos(CandleTimeframe timeframe);

    /**
     * Вернуть значение лейбла для индекса.
     */
    BigDecimal getValue(CandleTimeframe timeframe, int index);
}
