package artskif.trader.strategy.event.common;

public enum TradeEventType {

    /** Подтверждённый откат в тренде */
    PULLBACK,

    /** Пробой диапазона / уровня */
    BREAKOUT,

    /** Ложный пробой */
    FALSE_BREAKOUT,

    /** Отмена ранее ожидаемого события */
    EVENT_CANCELLED
}
