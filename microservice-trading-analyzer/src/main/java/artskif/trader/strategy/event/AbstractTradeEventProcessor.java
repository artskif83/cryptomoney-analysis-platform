package artskif.trader.strategy.event;

import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import jakarta.inject.Inject;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;

import java.util.Optional;

/**
 * Абстрактный базовый класс для процессоров торговых событий.
 * Предоставляет стандартную реализацию для методов проверки условий входа и выхода.
 */
public abstract class AbstractTradeEventProcessor implements TradeEventProcessor {

    @Inject
    protected ClosePriceIndicatorM closePriceIndicatorM;

    /**
     * Проверить, произошел ли торговый сигнал на данном баре
     *
     * @param index индекс бара для проверки
     * @return данные торгового сигнала, если он произошел, или пустой Optional иначе
     */
    @Override
    public Optional<TradeEventData> checkLifeTradeEvent(int index) {
        if (shouldEnter(index, null, true)) {
            Num value = closePriceIndicatorM.getIndicator(getTimeframe(), true).getValue(index);

            return Optional.of(new TradeEventData(
                    getTradeEventType(),
                    getTradeDirection(),
                    getStopLossPercentage().bigDecimalValue(),
                    getTakeProfitPercentage().bigDecimalValue(),
                    getTimeframe(),
                    value.bigDecimalValue()
            ));
        }

        return Optional.empty();
    }

    /**
     * Проверить, удовлетворяет ли текущий бар условиям входа в сделку
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord запись торговых операций
     * @param isLiveSeries  флаг использования live серии
     * @return true если условия входа выполнены, false иначе
     */
    @Override
    public boolean shouldEnter(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return getEntryRule(isLiveSeries).isSatisfied(index, tradingRecord);
    }

    /**
     * Проверить, удовлетворяет ли текущий бар условиям выхода из сделки
     *
     * @param index         индекс бара для проверки
     * @param tradingRecord запись торговых операций
     * @param isLiveSeries  флаг использования live серии
     * @return true если условия выхода выполнены, false иначе
     */
    @Override
    public boolean shouldExit(int index, TradingRecord tradingRecord, boolean isLiveSeries) {
        return getFixedExitRule(isLiveSeries).isSatisfied(index, tradingRecord);
    }
}
