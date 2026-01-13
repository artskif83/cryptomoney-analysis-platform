package artskif.trader.strategy;

import artskif.trader.candle.CandleTimeframe;

public abstract class AbstractStrategy {

    public abstract void onBar();

    protected abstract CandleTimeframe getTimeframe();
}
