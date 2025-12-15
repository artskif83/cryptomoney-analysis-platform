package artskif.trader.contract.features;

import artskif.trader.entity.Candle;

import java.util.List;

/**
 * Контекст для расчета RSI фичи
 */
public class RsiFeatureContext {

    private final List<Candle> candles;
    private final Candle currentCandle;

    public RsiFeatureContext(List<Candle> candles, Candle currentCandle) {
        this.candles = candles;
        this.currentCandle = currentCandle;
    }

    public List<Candle> getCandles() {
        return candles;
    }

    public Candle getCurrentCandle() {
        return currentCandle;
    }
}

