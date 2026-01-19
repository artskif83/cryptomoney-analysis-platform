package artskif.trader.events.trade;

import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.regime.common.MarketRegime;

import java.time.Instant;

public record TradeEvent(
        TradeEventType type,
        String instrument,
        Direction direction,
        Confidence confidence,
        MarketRegime regime,
        Instant timestamp
) {
    @Override
    public String toString() {
        return "TradeEvent{" + type + ", " + instrument + ", " + direction + ", " + confidence + ", " + regime + ", " + timestamp + "}";
    }
}


