package artskif.trader.repository;

import artskif.trader.indicator.adx.AdxPoint;
import artskif.trader.indicator.rsi.RsiPoint;

import java.time.Instant;
import java.util.Map;

public class AdxIndicatorRepository implements BufferRepository<AdxPoint>{
    @Override
    public boolean saveFromMap(Map<Instant, AdxPoint> buffer) {
        return false;
    }

    @Override
    public Map<Instant, AdxPoint> restoreFromStorage() {
        return Map.of();
    }
}
