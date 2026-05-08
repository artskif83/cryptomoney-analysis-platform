package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.event.common.TradeEventType;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;

import java.util.Optional;

public interface TradeEventProcessor {

    /**
     * Проверить, произошел ли торговый сигнал на данном баре
     *
     * @param index индекс бара для проверки
     * @return данные торгового сигнала, если он произошел
     */
    TradeEventData getLifeTradeEventData(int index);


    /**
     * Проверить, удовлетворяет ли текущий бар условиям входа в сделку
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord
     * @return true если условия входа выполнены, false иначе
     */
    boolean shouldMarketEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries);

    /**
     * Проверить, удовлетворяет ли текущий бар условиям выхода из сделки
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord
     * @return true если условия выхода выполнены, false иначе
     */
    boolean shouldMarketExit(int index, TradingRecord tradingRecord, boolean isLiveSeries);


    /**
     * Получить направление сделки (лонг или шорт)
     * @return тип сделки
     */
    Direction getTradeDirection();

    /**
     * Получить таймфрейм модели
     */
    CandleTimeframe getTimeframe();


    /**
     * Получить высший таймфрейм модели
     */
    CandleTimeframe getHighTimeframe();

    /**
     * Получить старший таймфрейм модели (если используется мульти-таймфрейм анализ)
     */
    TradeEventType getTradeEventType();

    /**
     * Получить силу тренда для текущего бара
     *
     * @param index         индекс бара для проверки
     * @param isLiveSeries  флаг, указывающий, является ли серия живой (для получения данных в реальном времени)
     * @return сила тренда в виде целого числа
     */
    Integer getTrendStrength(int index, boolean isLiveSeries);
}
