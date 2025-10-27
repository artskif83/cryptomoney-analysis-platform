package artskif.trader.repository;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;

/**
 * Дополнительные операции репозитория свечей.
 */
public interface BufferRepository {

    /**
     * Мапит {@link CandlestickDto} в {@link Candle} и сохраняет в БД.
     *
     * @param dto DTO свечи, содержащее все необходимые поля, включая period и instrument
     * @return сохранённая сущность Candle
     */
    Candle saveFromDto(CandlestickDto dto);
}
