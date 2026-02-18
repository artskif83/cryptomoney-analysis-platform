package artskif.trader.strategy.event.common;

import artskif.trader.candle.CandleTimeframe;

import java.math.BigDecimal;

public record TradeEventData(
        TradeEventType type,
        Direction direction,
        BigDecimal stopLossPercentage,
        BigDecimal takeProfitPercentage,
        CandleTimeframe timeframe,
        BigDecimal eventPrice
) {}
