package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ResistanceLevelIndicator;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ResistanceLevelIndicatorM extends MultiAbstractIndicator<ResistanceLevelIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final CandleResistanceStrengthM candleResistanceStrengthM;
    private final int barCount;
    private final DecimalNum resistanceRangePercentagesThreshold;

    // No-args constructor required by CDI
    protected ResistanceLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.candleResistanceStrengthM = null;
        this.barCount = 32;
        this.resistanceRangePercentagesThreshold = DecimalNum.valueOf(0.002);
    }

    @Inject
    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM) {
        this(candle, highPriceIndicatorM, candleResistanceStrengthM, 32, DecimalNum.valueOf(0.002));
    }

    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM,
                                     int barCount,
                                     DecimalNum resistanceRangePercentagesThreshold) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.candleResistanceStrengthM = candleResistanceStrengthM;
        this.barCount = barCount;
        this.resistanceRangePercentagesThreshold = resistanceRangePercentagesThreshold;
    }

    @Override
    protected ResistanceLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ResistanceLevelIndicator(
                highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                barCount,
                resistanceRangePercentagesThreshold
        );
    }
}
