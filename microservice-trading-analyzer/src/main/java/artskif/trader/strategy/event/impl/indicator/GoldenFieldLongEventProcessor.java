package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.AbstractTradeEventProcessor;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.indicators.base.LongTrendIndicator;
import artskif.trader.strategy.indicators.multi.LowPriceIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.LongTrendIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

@ApplicationScoped
public class GoldenFieldLongEventProcessor extends AbstractTradeEventProcessor {

    private final LongTrendIndicatorM longTrendIndicatorM;
    private final LowPriceIndicatorM lowPriceIndicatorM;

    public GoldenFieldLongEventProcessor() {
        this.longTrendIndicatorM = null;
        this.lowPriceIndicatorM = null;
    }

    @Inject
    public GoldenFieldLongEventProcessor(LongTrendIndicatorM longTrendIndicatorM,
                                         LowPriceIndicatorM lowPriceIndicatorM) {
        this.longTrendIndicatorM = longTrendIndicatorM;
        this.lowPriceIndicatorM = lowPriceIndicatorM;
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
        LongTrendIndicator indicator = longTrendIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator != null && indicator.getValue(index) != null;
    }

    @Override
    public boolean shouldLimitExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public Num getEntryPrice(int index, boolean isLiveSeries) {
        LongTrendIndicator indicator = longTrendIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator != null && indicator.getValue(index) != null ? indicator.getValue(index) : null;
    }

    @Override
    public Direction getTradeDirection() {
        return Direction.LONG;
    }

    @Override
    public Num getStopLossPercentage() {
        return DecimalNum.valueOf(0.15);
    }

    @Override
    public Num getTakeProfitPercentage() {
        return DecimalNum.valueOf(2);
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
