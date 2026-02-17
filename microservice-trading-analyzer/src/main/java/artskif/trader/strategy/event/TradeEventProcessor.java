package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.TradeEventData;
import org.ta4j.core.Rule;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;

import java.util.Optional;

public interface TradeEventProcessor {
    /**
     * Детектировать торговое событие на основе снимка контракта и режима рынка
     * @return Optional с событием, если модель поддерживает данный режим и событие обнаружено
     */
    Optional<TradeEventData> detect();

    /**
     * Получить правило входа в позицию
     * @return правило для определения точки входа в сделку
     */
    Rule getEntryRule(boolean isLiveSeries);

    /**
     * Получить правило выхода из позиции
     * @return правило для определения точки выхода
     */
    Rule getFixedExitRule(boolean isLiveSeries);


    /**
     * Проверить, удовлетворяет ли текущий бар условиям входа в сделку
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord
     * @return true если условия входа выполнены, false иначе
     */
    boolean shouldEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries);

    /**
     * Проверить, удовлетворяет ли текущий бар условиям выхода из сделки
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord
     * @return true если условия выхода выполнены, false иначе
     */
    boolean shouldExit(int index, TradingRecord tradingRecord, boolean isLiveSeries);

    /**
     * Получить тип сделки (лонг или шорт)
     * @return тип сделки
     */
    Trade.TradeType getTradeType();

    /**
     * Получить процент для установки стоп-лосса от цены входа
     * @return процент для стоп-лосса
     */
    Num getStoplossPercentage();

    /**
     * Получить процент для установки тейк-профита от цены входа
     * @return процент для тейк-профита
     */
    Num getTakeprofitPercentage();


    /**
     * Получить таймфрейм модели
     */
    CandleTimeframe getTimeframe();

    /**
     * Получить старший таймфрейм модели (если используется мульти-таймфрейм анализ)
     */
    CandleTimeframe getHigherTimeframe();
}
