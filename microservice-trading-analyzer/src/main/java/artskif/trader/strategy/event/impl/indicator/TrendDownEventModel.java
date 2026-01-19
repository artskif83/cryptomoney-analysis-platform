package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEvent;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.regime.common.MarketRegime;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TrendDownEventModel implements EventModel {

    @Override
    public Optional<TradeEvent> detect(ContractSnapshot snapshot, MarketRegime regime) {
        // Проверяем, что текущий режим соответствует поддерживаемому режиму
        if (regime != getSupportedRegime()) {
            return Optional.empty();
        }

        // Логика детектирования события для нисходящего тренда
        return Optional.of(new TradeEvent(TradeEventType.FALSE_BREAKOUT, Direction.LONG, Confidence.HIGH));
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

    @Override
    public MarketRegime getSupportedRegime() {
        return MarketRegime.TREND_DOWN;
    }
}
