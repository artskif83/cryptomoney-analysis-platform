package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.event.AbstractTradeEventProcessor;
import artskif.trader.strategy.indicators.multi.TripleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.ResistanceLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;


@ApplicationScoped
public class GoldenFieldEventProcessor extends AbstractTradeEventProcessor {

    private final TripleMAIndicatorM tripleMAIndicatorM;
    private final ResistanceLevelIndicatorM resistanceLevelIndicatorM;
    private final HighPriceIndicatorM highPriceIndicatorM;

    public GoldenFieldEventProcessor() {
        this.tripleMAIndicatorM = null;
        this.resistanceLevelIndicatorM = null;
        this.highPriceIndicatorM = null;
    }

    @Inject
    public GoldenFieldEventProcessor(TripleMAIndicatorM tripleMAIndicatorM,
                                     ResistanceLevelIndicatorM resistanceLevelIndicatorM,
                                     HighPriceIndicatorM highPriceIndicatorM) {
        this.tripleMAIndicatorM = tripleMAIndicatorM;
        this.resistanceLevelIndicatorM = resistanceLevelIndicatorM;
        this.highPriceIndicatorM = highPriceIndicatorM;
    }


    @Override
    public Rule getEntryRule(boolean isLiveSeries) {
        var tripleMARule = new IsEqualRule(tripleMAIndicatorM.getIndicator(getTimeframe(), isLiveSeries), -2);
        OverIndicatorRule resistance1m = new OverIndicatorRule(resistanceLevelIndicatorM.getIndicator(getTimeframe(), isLiveSeries), 5);

        return tripleMARule.and(resistance1m);
    }

    @Override
    public Rule getFixedExitRule(boolean isLiveSeries) {
        HighPriceIndicator indicator = highPriceIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return new StopLossRule(indicator, getStopLossPercentage().bigDecimalValue())
                .or(new StopGainRule(indicator, getTakeProfitPercentage().bigDecimalValue()));
    }


    @Override
    public Direction getTradeDirection() {
        return Direction.SHORT;
    }

    @Override
    public Num getStopLossPercentage() {
        return DecimalNum.valueOf(0.05);
    }

    @Override
    public Num getTakeProfitPercentage() {
        return DecimalNum.valueOf(1);
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public CandleTimeframe getHigherTimeframe() {
        return CandleTimeframe.CANDLE_4H;
    }

    @Override
    public TradeEventType getTradeEventType() {
        return TradeEventType.GOLDEN_FIELD;
    }

}

