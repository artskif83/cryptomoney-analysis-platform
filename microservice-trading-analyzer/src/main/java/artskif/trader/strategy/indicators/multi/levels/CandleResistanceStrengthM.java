package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.CandleResistanceStrength;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class CandleResistanceStrengthM extends MultiAbstractIndicator<CandleResistanceStrength> {

    // No-args constructor required by CDI
    protected CandleResistanceStrengthM() {
        super(null);
    }

    @Inject
    public CandleResistanceStrengthM(Candle candle) {
        super(candle);
    }

    @Override
    protected CandleResistanceStrength createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new CandleResistanceStrength(getBarSeries(timeframe, isLifeSeries), DecimalNum.valueOf(0.03));
    }
}
