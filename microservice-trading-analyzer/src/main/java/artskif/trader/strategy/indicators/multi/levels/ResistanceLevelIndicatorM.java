package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ResistanceLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ResistanceLevelIndicatorM extends MultiAbstractIndicator<ResistanceLevelIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final CandleResistanceStrengthM candleResistanceStrengthM;
    private final int barCount;
    private final DecimalNum resistanceRangePercentagesThreshold;
    private final DecimalNum resistanceZonePercentagesThreshold;

    // No-args constructor required by CDI
    protected ResistanceLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.candleResistanceStrengthM = null;
        this.barCount = 0;
        this.resistanceRangePercentagesThreshold = null;
        this.resistanceZonePercentagesThreshold = null;
    }

    @Inject
    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     ClosePriceIndicatorM closePriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM) {
        this(candle, highPriceIndicatorM, closePriceIndicatorM, candleResistanceStrengthM, 12, DecimalNum.valueOf(0.001), DecimalNum.valueOf(0.005));
    }

    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     ClosePriceIndicatorM closePriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM,
                                     int barCount,
                                     DecimalNum resistanceRangePercentagesThreshold,
                                     DecimalNum resistanceZonePercentagesThreshold) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.candleResistanceStrengthM = candleResistanceStrengthM;
        this.barCount = barCount;
        this.resistanceRangePercentagesThreshold = resistanceRangePercentagesThreshold;
        this.resistanceZonePercentagesThreshold = resistanceZonePercentagesThreshold;
    }

    @Override
    protected ResistanceLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ResistanceLevelIndicator(
                highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                barCount,
                resistanceRangePercentagesThreshold,
                resistanceZonePercentagesThreshold
        );
    }
}
