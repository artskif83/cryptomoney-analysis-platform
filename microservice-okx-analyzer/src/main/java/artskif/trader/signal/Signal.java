package artskif.trader.signal;

import artskif.trader.candle.CandleTimeframe;

import java.time.Instant;

/** Итоговый торговый сигнал. */
public record Signal(
        Instant time,                 // время сигнала
        OperationType operation,      // BUY / SELL
        StrategyKind strategy,        // например: ADX_RSI
        Integer level,                // например: 15, 20, 30 и т.д.
        TrendDirection direction,     // например: UP или DOWN
        CandleTimeframe timeframe     // на каком таймфрейме он зафиксирован
) {}
