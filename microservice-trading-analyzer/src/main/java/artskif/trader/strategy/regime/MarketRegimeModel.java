package artskif.trader.strategy.regime;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.regime.common.MarketRegime;

public interface MarketRegimeModel {
    MarketRegime classify(ContractSnapshot snapshot);

    CandleTimeframe getTimeframe();
}
