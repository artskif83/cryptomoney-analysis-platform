package artskif.trader.signal;

import artskif.trader.candle.CandleTimeframe;

import java.time.Instant;

/** Итоговый торговый сигнал. */
public record Signal(
        Instant time,                 // время сигнала
        OperationType operation,      // BUY / SELL
        SignalType type,              // например: (RSI, 14)
        CandleTimeframe timeframe     // на каком таймфрейме он зафиксирован
) {}
