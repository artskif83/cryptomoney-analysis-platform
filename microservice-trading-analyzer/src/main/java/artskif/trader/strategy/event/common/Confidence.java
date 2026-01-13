package artskif.trader.strategy.event.common;

public enum Confidence {

    /** Слабый сигнал, допустим только при агрессивном риске */
    LOW,

    /** Базовый рабочий сигнал */
    MEDIUM,

    /** Сильный сигнал, приоритетный */
    HIGH
}
