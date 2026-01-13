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
     * Вернуть значение лейбла для индекса.
     */
    BigDecimal getValue(CandleTimeframe timeframe, int index);
}
