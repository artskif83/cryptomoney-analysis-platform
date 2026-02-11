package artskif.trader.events.trade;

import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.TradeEventType;

import java.time.Instant;

public record TradeEvent(
        TradeEventType type,
        String instrument,
        Direction direction,
        Confidence confidence,
        Instant timestamp,
        Boolean isTest
) {
    @Override
    public String toString() {
        return "TradeEvent{" + type + ", " + instrument + ", " + direction + ", " + confidence + ", " + timestamp + ", isTest=" + isTest + "}";
    }
}


