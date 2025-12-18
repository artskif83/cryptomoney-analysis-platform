package artskif.trader.contract.labels;

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
    List<CandlestickDto> getCandlestickDtos();

    /**
     * Вернуть значение лейбла для индекса.
     */
    BigDecimal getValue(int index);
}
