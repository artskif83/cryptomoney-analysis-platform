package artskif.trader.repository;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId>, BufferRepository {

    @Override
    public Candle saveFromDto(CandlestickDto dto) {
        if (dto == null) return null;

        Instant ts = dto.getTimestamp();
        CandleId id = new CandleId(dto.getInstrument(), dto.getPeriod().name(), ts);

        Candle candle = new Candle(
                id,
                dto.getOpen(),
                dto.getHigh(),
                dto.getLow(),
                dto.getClose(),
                dto.getVolume() != null ? dto.getVolume() : java.math.BigDecimal.ZERO,
                dto.getVolumeCcy() != null ? dto.getVolumeCcy() : java.math.BigDecimal.ZERO,
                dto.getVolumeCcyQuote() != null ? dto.getVolumeCcyQuote() : java.math.BigDecimal.ZERO,
                Boolean.TRUE.equals(dto.getConfirmed())
        );
        // Сохраняем сущность через Panache
        persist(candle);
        return candle;
    }
}
