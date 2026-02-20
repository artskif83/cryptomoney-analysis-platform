package artskif.trader.events.trade;

import artskif.trader.strategy.event.common.TradeEventData;

import java.time.Instant;

public record TradeEvent(
        TradeEventData tradeEventData,
        String instrument,
        Instant timestamp,
        Boolean isTest
) {
    @Override
    public String toString() {
        return "TradeEvent{" +
                "type=" + tradeEventData.type() +
                ", instrument='" + instrument + '\'' +
                ", direction=" + tradeEventData.direction() +
                ", stopLossPercentage=" + tradeEventData.stopLossPercentage() +
                ", takeProfitPercentage=" + tradeEventData.takeProfitPercentage() +
                ", timeframe=" + tradeEventData.timeframe() +
                ", eventPrice=" + tradeEventData.eventPrice() +
                ", timestamp=" + timestamp +
                ", isTest=" + isTest +
                '}';
    }
}


