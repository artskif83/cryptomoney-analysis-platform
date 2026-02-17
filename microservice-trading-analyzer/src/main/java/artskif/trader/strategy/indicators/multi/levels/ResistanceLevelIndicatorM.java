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

    // No-args constructor required by CDI
    protected ResistanceLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.candleResistanceStrengthM = null;
    }

    @Inject
    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     ClosePriceIndicatorM closePriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.candleResistanceStrengthM = candleResistanceStrengthM;

    }

    @Override
    protected ResistanceLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {

        return switch (timeframe) {
            case CANDLE_5M -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                    12,
                    DecimalNum.valueOf(0.001),
                    DecimalNum.valueOf(0.005)
            );
            case CANDLE_1M -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                    36,
                    DecimalNum.valueOf(0.001),
                    DecimalNum.valueOf(0.005)
            );
            case CANDLE_4H -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                    12,
                    DecimalNum.valueOf(0.003),
                    DecimalNum.valueOf(0.01)
            );
            default -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    candleResistanceStrengthM.getIndicator(timeframe, isLifeSeries),
                    12,
                    DecimalNum.valueOf(0.005),
                    DecimalNum.valueOf(0.025)
            );
        };
    }
}
