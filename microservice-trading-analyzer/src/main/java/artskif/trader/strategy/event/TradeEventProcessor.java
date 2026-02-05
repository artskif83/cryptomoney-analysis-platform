package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.regime.common.MarketRegime;
import org.ta4j.core.Rule;

import java.util.Optional;

public interface TradeEventProcessor {
    /**
     * Детектировать торговое событие на основе снимка контракта и режима рынка
     * @param regime текущий режим рынка
     * @return Optional с событием, если модель поддерживает данный режим и событие обнаружено
     */
    Optional<TradeEventData> detect(MarketRegime regime);

    /**
     * Получить правило входа в позицию
     * @return правило для определения точки входа в сделку
     */
    Rule getEntryRule(boolean isLiveSeries);

    /**
     * Получить правило выхода из позиции
     * @return правило для определения точки выхода
     */
    Rule getFixedExitRule(boolean isLiveSeries, Number lossPercentage, Number gainPercentage);

    /**
     * Получить таймфрейм модели
     */
    CandleTimeframe getTimeframe();
}
