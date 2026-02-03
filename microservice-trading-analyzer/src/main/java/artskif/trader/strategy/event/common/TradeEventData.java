package artskif.trader.strategy.event.common;

public record TradeEventData(
        TradeEventType type,
        Direction direction,
        Confidence confidence
) {}
