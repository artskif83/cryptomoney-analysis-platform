package artskif.trader.strategy.event;

import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import jakarta.inject.Inject;
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
     * @return данные торгового сигнала, если он произошел
     */
    @Override
    public TradeEventData getLifeTradeEventData(int index) {

        return new TradeEventData(
                getTradeEventType(),
                getTradeDirection(),
                getStopLossPercentage().bigDecimalValue(),
                getTakeProfitPercentage().bigDecimalValue(),
                getTimeframe(),
                getEntryPrice(index, true).bigDecimalValue()
        );
    }
}
