package artskif.trader.signal;

/**
 * Тип сигнала = семейство + параметр (например, период RSI).
 * Это гибче, чем перечислять все периоды отдельными enum-ами.
 */
public record SignalType(StrategyKind family, int period) {
    public static SignalType rsi(int period) {
        return new SignalType(StrategyKind.TRIPLE_RSI, period);
    }
}
