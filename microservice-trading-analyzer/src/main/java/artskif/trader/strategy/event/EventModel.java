package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.event.common.TradeEvent;
import artskif.trader.strategy.regime.common.MarketRegime;

import java.util.Optional;

public interface EventModel {
    /**
     * Детектировать торговое событие на основе снимка контракта и режима рынка
     * @param snapshot снимок контракта
     * @param regime текущий режим рынка
     * @return Optional с событием, если модель поддерживает данный режим и событие обнаружено
     */
    Optional<TradeEvent> detect(ContractSnapshot snapshot, MarketRegime regime);

    /**
     * Получить таймфрейм модели
     */
    CandleTimeframe getTimeframe();

    /**
     * Получить режим рынка, за который отвечает данная модель
     */
    MarketRegime getSupportedRegime();
}
