package artskif.trader.repository;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.indicator.rsi.RsiPoint;

import java.time.Instant;
import java.util.Map;

public class RsiIndicatorRepository implements BufferRepository<RsiPoint>{

    @Override
    public boolean saveFromMap(Map<Instant, RsiPoint> buffer) {
        return false;
    }

    @Override
    public Map<Instant, RsiPoint> restoreFromStorage() {
        return Map.of();
    }
}
