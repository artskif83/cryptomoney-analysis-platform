package artskif.trader.repository;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import artskif.trader.mapper.CandlestickMapper;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId>, BufferRepository<CandlestickDto> {

    public Candle saveFromDto(CandlestickDto dto) {
        if (dto == null) return null;

        // делегируем мапинг
        Candle candle = CandlestickMapper.mapDtoToEntity(dto);

        // Сохраняем сущность через Panache
        persist(candle);
        return candle;
    }

    @Override
    public boolean saveFromMap(Map<Instant, CandlestickDto> buffer) {
        return false;
    }

    @Override
    public Map<Instant, CandlestickDto> restoreFromStorage() {
        return Map.of();
    }
}
