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
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
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
        Rule entryRule = getEntryRule(true);
//        ContractSnapshot snapshot5m =
//                snapshotBuilder.build(schema5mBase, lastIndex5m, true);


        // Логика детектирования события для флетового рынка
        // Например, можно искать ложные пробои или отскоки от границ диапазона
        // TODO: Реализовать логику детектирования событий во флете

        return Optional.empty(); // Пока ничего не детектируем
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
        return new StopLossRule(indicator, getStoplossPercentage().bigDecimalValue())
                .or(new StopGainRule(indicator, getTakeprofitPercentage().bigDecimalValue()));
    }

    @Override
    public boolean shouldEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return getEntryRule(isLiveSeries).isSatisfied(index, tradingRecord);
    }

    @Override
    public boolean shouldExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return getFixedExitRule(isLiveSeries).isSatisfied(index, tradingRecord);
    }

    @Override
    public Trade.TradeType getTradeType() {
        return Trade.TradeType.SELL;
    }

    @Override
    public Num getStoplossPercentage() {
        return DecimalNum.valueOf(0.05);
    }

    @Override
    public Num getTakeprofitPercentage() {
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

}

