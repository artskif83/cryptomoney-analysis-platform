package artskif.trader.strategy.regime;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.regime.common.MarketRegime;

public interface MarketRegimeProcessor {
    MarketRegime classify();

    CandleTimeframe getTimeframe();
}
