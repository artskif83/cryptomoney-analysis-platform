package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.event.common.TradeEvent;
import artskif.trader.strategy.regime.common.MarketRegime;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Модель событий для режима флета (бокового движения рынка)
 * Отвечает только за детектирование событий во время FLAT режима
 */
@ApplicationScoped
public class FlatEventModel implements EventModel {

    @Override
    public Optional<TradeEvent> detect(ContractSnapshot snapshot, MarketRegime regime) {
        // Проверяем, что текущий режим соответствует поддерживаемому режиму
        if (regime != getSupportedRegime()) {
            return Optional.empty();
        }

        // Логика детектирования события для флетового рынка
        // Например, можно искать ложные пробои или отскоки от границ диапазона
        // TODO: Реализовать логику детектирования событий во флете

        return Optional.empty(); // Пока ничего не детектируем
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

    @Override
    public MarketRegime getSupportedRegime() {
        return MarketRegime.FLAT;
    }
}

