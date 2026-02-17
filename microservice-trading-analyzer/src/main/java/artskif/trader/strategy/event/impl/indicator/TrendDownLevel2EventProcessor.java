package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.event.TradeEventProcessor;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.indicators.multi.TripleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.ResistanceLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.rules.*;

import java.util.Optional;

@ApplicationScoped
public class TrendDownLevel2EventProcessor implements TradeEventProcessor {

    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final TripleMAIndicatorM tripleMAIndicatorM;
    private final ResistanceLevelIndicatorM resistanceLevelIndicatorM;
    private final HighPriceIndicatorM highPriceIndicatorM;

    public TrendDownLevel2EventProcessor() {
        this.closePriceIndicatorM = null;
        this.tripleMAIndicatorM = null;
        this.resistanceLevelIndicatorM = null;
        this.highPriceIndicatorM = null;
    }

    @Inject
    public TrendDownLevel2EventProcessor(TripleMAIndicatorM tripleMAIndicatorM,
                                         ClosePriceIndicatorM closePriceIndicatorM,
                                         ResistanceLevelIndicatorM resistanceLevelIndicatorM,
                                         HighPriceIndicatorM highPriceIndicatorM) {
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.tripleMAIndicatorM = tripleMAIndicatorM;
        this.resistanceLevelIndicatorM = resistanceLevelIndicatorM;
        this.highPriceIndicatorM = highPriceIndicatorM;
    }

    @Override
    public Optional<TradeEventData> detect() {

//        ContractSnapshot snapshot5m =
//                snapshotBuilder.build(schema5mBase, lastIndex5m, true);


        // Логика детектирования события для флетового рынка
        // Например, можно искать ложные пробои или отскоки от границ диапазона
        // TODO: Реализовать логику детектирования событий во флете

        return Optional.empty(); // Пока ничего не детектируем
    }

    @Override
    public Rule getEntryRule(boolean isLiveSeries) {
        var tripleMARule = new IsEqualRule(tripleMAIndicatorM.getHigherTimeframeIndicator(getTimeframe(),getHigherTimeframe(), isLiveSeries), -2);
        OverIndicatorRule resistance1m = new OverIndicatorRule(resistanceLevelIndicatorM.getIndicator(getTimeframe(), isLiveSeries), 3);

        return tripleMARule.and(resistance1m);
    }

    @Override
    public Rule getFixedExitRule(boolean isLiveSeries, Number lossPercentage, Number gainPercentage) {
        HighPriceIndicator indicator = highPriceIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return new StopLossRule(indicator, lossPercentage)
                .or(new StopGainRule(indicator, gainPercentage));
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public CandleTimeframe getHigherTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

}

