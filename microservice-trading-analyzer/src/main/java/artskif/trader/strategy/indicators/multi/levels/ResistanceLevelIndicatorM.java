package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ResistanceLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ResistanceLevelIndicatorM extends MultiAbstractIndicator<ResistanceLevelIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final CandleResistanceStrengthM candleResistanceStrengthM;
    private final DoubleMAIndicatorM doubleMAIndicatorM;


    // No-args constructor required by CDI
    protected ResistanceLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.candleResistanceStrengthM = null;
        this.doubleMAIndicatorM = null;
    }

    @Inject
    public ResistanceLevelIndicatorM(Candle candle,
                                     HighPriceIndicatorM highPriceIndicatorM,
                                     ClosePriceIndicatorM closePriceIndicatorM,
                                     CandleResistanceStrengthM candleResistanceStrengthM,
                                     DoubleMAIndicatorM doubleMAIndicatorM) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.candleResistanceStrengthM = candleResistanceStrengthM;
        this.doubleMAIndicatorM = doubleMAIndicatorM;
    }

    @Override
    protected ResistanceLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {

        return switch (timeframe) {
            case CANDLE_1M -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                    doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    4,
                    5,
                    DecimalNum.valueOf(0.1),
                    DecimalNum.valueOf(0.05),
                    DecimalNum.valueOf(0.3)
            );
            case CANDLE_5M -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                    doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    4,
                    6,
                    DecimalNum.valueOf(0.1),
                    DecimalNum.valueOf(0.1),
                    DecimalNum.valueOf(0.3)
            );
            default -> new ResistanceLevelIndicator(
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                    highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                    doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                    closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                    4,
                    6,
                    DecimalNum.valueOf(0.1),
                    DecimalNum.valueOf(0.1),
                    DecimalNum.valueOf(0.3)
            );
        };
    }
}
