package artskif.trader.events.regime;

import artskif.trader.strategy.regime.common.MarketRegime;

import java.time.Instant;

public record RegimeChangeEvent(
        String instrument,
        MarketRegime previousRegime,
        MarketRegime currentRegime,
        Instant timestamp,
        Boolean isTest
) {
    @Override
    public String toString() {
        return "RegimeChangeEvent{" + instrument + ", " + previousRegime + " -> " + currentRegime + ", " + timestamp + ", isTest=" + isTest + "}";
    }
}

