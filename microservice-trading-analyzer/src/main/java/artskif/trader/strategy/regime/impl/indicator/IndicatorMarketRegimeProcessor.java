package artskif.trader.strategy.regime.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.regime.common.MarketRegime;
import artskif.trader.strategy.regime.MarketRegimeProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndicatorMarketRegimeProcessor implements MarketRegimeProcessor {

    @Override
    public MarketRegime classify() {
        // RSI, ADX, slope — без знания стратегии
//        ContractSnapshot snapshot4h =
//                snapshotBuilder.build(schema4hBase, lastIndex, true);


        return MarketRegime.TREND_DOWN;
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_4H;
    }
}
