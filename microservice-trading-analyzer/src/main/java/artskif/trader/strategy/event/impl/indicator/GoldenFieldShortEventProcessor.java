package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.indicators.base.MultiMAIndicator;
import artskif.trader.strategy.indicators.base.ShortTrendIndicator;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.event.AbstractTradeEventProcessor;
import artskif.trader.strategy.indicators.multi.MultiMAIndicatorM;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import artskif.trader.strategy.indicators.multi.TripleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.ShortTrendIndicatorM;
import artskif.trader.strategy.indicators.util.IndicatorUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;


@ApplicationScoped
public class GoldenFieldShortEventProcessor extends AbstractTradeEventProcessor {

    @Override
    public boolean shouldMarketEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        RSIIndicator indicator = rsiIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return indicator.getValue(index) != null && indicator.getValue(index).isGreaterThan(DecimalNum.valueOf(70));
    }

    @Override
    public boolean shouldMarketExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return false;
    }

    @Override
    public Direction getTradeDirection() {
        return Direction.SHORT;
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

