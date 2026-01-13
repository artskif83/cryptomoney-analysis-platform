package artskif.trader.strategy.regime.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.regime.common.MarketRegime;
import artskif.trader.strategy.regime.MarketRegimeModel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndicatorMarketRegimeModel implements MarketRegimeModel {

    @Override
    public MarketRegime classify(ContractSnapshot snapshot) {
        // RSI, ADX, slope — без знания стратегии
        return MarketRegime.FLAT;
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_4H;
    }
}
