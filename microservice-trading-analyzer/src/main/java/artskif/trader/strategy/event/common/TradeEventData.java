package artskif.trader.strategy.event.common;

import artskif.trader.candle.CandleTimeframe;

import java.math.BigDecimal;

public record TradeEventData(
        TradeEventType type,
        Direction direction,
        CandleTimeframe timeframe,
        Integer trendStrength
) {}
