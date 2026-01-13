package artskif.trader.strategy.event.common;

public record TradeEvent(
        TradeEventType type,
        Direction direction,
        Confidence confidence
) {}
