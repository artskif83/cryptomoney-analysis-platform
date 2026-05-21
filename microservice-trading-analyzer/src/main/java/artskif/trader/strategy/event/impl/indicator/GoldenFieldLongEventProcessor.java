package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.AbstractTradeEventProcessor;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class GoldenFieldLongEventProcessor extends AbstractTradeEventProcessor {


    @Override
    public boolean shouldMarketEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        RSIIndicator indicator = rsiIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator.getValue(index) != null && indicator.getValue(index).isLessThan(DecimalNum.valueOf(31));
    }

    @Override
    public boolean shouldMarketExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public Direction getTradeDirection() {
        return Direction.LONG;
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public CandleTimeframe getHighTimeframe() {
        return CandleTimeframe.CANDLE_1H;
    }

    @Override
    public TradeEventType getTradeEventType() {
        return TradeEventType.GOLDEN_FIELD;
    }

}
