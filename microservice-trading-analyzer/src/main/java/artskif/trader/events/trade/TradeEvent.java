package artskif.trader.events.trade;

import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.event.common.TradeEventType;

import java.time.Instant;

public record TradeEvent(
        TradeEventData tradeEventData,
        String instrument,
        Instant timestamp,
        Boolean isTest
) {
    @Override
    public String toString() {
        return "TradeEvent{" + tradeEventData.type() + ", " + instrument + ", " + tradeEventData.direction() + ", " + timestamp + ", isTest=" + isTest + "}";
    }
}


