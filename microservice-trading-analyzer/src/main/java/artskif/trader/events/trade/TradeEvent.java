package artskif.trader.events.trade;

import artskif.trader.strategy.event.common.TradeEventData;

import java.time.Instant;

public record TradeEvent(
        TradeEventData tradeEventData,
        String instrument,
        String tag,
        Instant timestamp,
        Boolean isTest
) {
    @Override
    public String toString() {
        return "TradeEvent{" +
                "type=" + tradeEventData.type() +
                ", instrument='" + instrument + '\'' +
                ", tag='" + tag + '\'' +
                ", direction=" + tradeEventData.direction() +
                ", timeframe=" + tradeEventData.timeframe() +
                ", timestamp=" + timestamp +
                ", isTest=" + isTest +
                ", tradeEventData=" + tradeEventData +
                '}';
    }
}


