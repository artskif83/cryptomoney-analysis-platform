package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.indicators.base.ADXAngleIndicator;
import artskif.trader.strategy.indicators.base.MultiMAIndicator;
import artskif.trader.strategy.indicators.multi.ADXAngleIndicatorM;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.MultiMAIndicatorM;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import artskif.trader.strategy.indicators.util.IndicatorUtils;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.Num;


/**
 * Абстрактный базовый класс для процессоров торговых событий.
 * Предоставляет стандартную реализацию для методов проверки условий входа и выхода.
 */
public abstract class AbstractTradeEventProcessor implements TradeEventProcessor {

    @Inject
    protected RSIIndicatorM rsiIndicatorM;

    @Inject
    protected MultiMAIndicatorM multiMAIndicatorM;

    @Inject
    protected ADXAngleIndicatorM adxAngleIndicatorM;
    /**
     * Получить силу тренда для текущего бара.
     * Возвращает 0 если один из индикаторов недоступен.
     */
    @Override
    public Integer getTrendStrength(int index, boolean isLiveSeries) {
        MultiMAIndicator multiMAIndicator = multiMAIndicatorM != null
                ? multiMAIndicatorM.getIndicator(getHighTimeframe(), isLiveSeries)
                : null;
        RSIIndicator rsiIndicator = rsiIndicatorM != null
                ? rsiIndicatorM.getIndicator(getTimeframe(), isLiveSeries)
                : null;

        if (multiMAIndicator == null || rsiIndicator == null) {
            return 0;
        }

        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(
                rsiIndicator.getBarSeries().getBar(index),
                multiMAIndicator.getBarSeries()
        );

        Num multiMAIndicatorValue = multiMAIndicator.getValue(higherTfIndex);
        return multiMAIndicatorValue != null ? multiMAIndicatorValue.intValue() : 0;
    }


    @Override
    public Integer getTrendStability(int index, boolean isLiveSeries) {
        ADXAngleIndicator adxAngleIndicator  = adxAngleIndicatorM != null
                ? adxAngleIndicatorM.getIndicator(CandleTimeframe.CANDLE_4H, isLiveSeries)
                : null;

        RSIIndicator rsiIndicator = rsiIndicatorM != null
                ? rsiIndicatorM.getIndicator(getTimeframe(), isLiveSeries)
                : null;

        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(
                rsiIndicator.getBarSeries().getBar(index),
                adxAngleIndicator.getBarSeries()
        );
        Num value = adxAngleIndicator.getValue(higherTfIndex);
        return value != null ? value.intValue() : 0;
    }

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
                getTimeframe(),
                getTrendStrength(index, true),
                getTrendStability(index, true)
        );
    }
}
