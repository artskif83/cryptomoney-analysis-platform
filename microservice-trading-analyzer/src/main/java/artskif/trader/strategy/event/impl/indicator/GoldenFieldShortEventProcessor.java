package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.indicators.base.ShortTrendIndicator;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.event.AbstractTradeEventProcessor;
import artskif.trader.strategy.indicators.multi.TripleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.ShortTrendIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;


@ApplicationScoped
public class GoldenFieldShortEventProcessor extends AbstractTradeEventProcessor {

    private final ShortTrendIndicatorM shortTrendIndicatorM;
    private final HighPriceIndicatorM highPriceIndicatorM;

    public GoldenFieldShortEventProcessor() {
        this.shortTrendIndicatorM = null;
        this.highPriceIndicatorM = null;
    }

    @Inject
    public GoldenFieldShortEventProcessor(TripleMAIndicatorM tripleMAIndicatorM,
                                          ShortTrendIndicatorM shortTrendIndicatorM,
                                          HighPriceIndicatorM highPriceIndicatorM) {
        this.shortTrendIndicatorM = shortTrendIndicatorM;
        this.highPriceIndicatorM = highPriceIndicatorM;
    }


    @Override
    public boolean shouldMarketEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public boolean shouldMarketExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public boolean shouldLimitEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        ShortTrendIndicator indicator = shortTrendIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator != null && indicator.getValue(index) != null;
    }

    @Override
    public boolean shouldLimitExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public Num getEntryPrice(int index, boolean isLiveSeries) {
        ShortTrendIndicator indicator = shortTrendIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator != null && indicator.getValue(index) != null ? indicator.getValue(index) : null;
    }

    @Override
    public Direction getTradeDirection() {
        return Direction.SHORT;
    }

    @Override
    public Num getStopLossPercentage() {
        return DecimalNum.valueOf(0.2);
    }

    @Override
    public Num getTakeProfitPercentage() {
        return DecimalNum.valueOf(14);
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public TradeEventType getTradeEventType() {
        return TradeEventType.GOLDEN_FIELD;
    }

}

