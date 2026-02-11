package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import artskif.trader.strategy.event.TradeEventProcessor;
import artskif.trader.strategy.event.common.TradeEventData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;

import java.util.Optional;

/**
 * Модель событий для режима флета (бокового движения рынка)
 * Отвечает только за детектирование событий во время FLAT режима
 */
@ApplicationScoped
public class TrendDownEventProcessor implements TradeEventProcessor {

    private final RSIIndicatorM rsiIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;

    public TrendDownEventProcessor() {
        this.rsiIndicatorM = null;
        this.closePriceIndicatorM = null;
    }

    @Inject
    public TrendDownEventProcessor(RSIIndicatorM rsiIndicatorM, ClosePriceIndicatorM closePriceIndicatorM) {
        this.rsiIndicatorM = rsiIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
    }

    @Override
    public Optional<TradeEventData> detect() {

//        ContractSnapshot snapshot5m =
//                snapshotBuilder.build(schema5mBase, lastIndex5m, true);


        // Логика детектирования события для флетового рынка
        // Например, можно искать ложные пробои или отскоки от границ диапазона
        // TODO: Реализовать логику детектирования событий во флете

        return Optional.empty(); // Пока ничего не детектируем
    }

    @Override
    public Rule getEntryRule(boolean isLiveSeries) {
        return new CrossedUpIndicatorRule(rsiIndicatorM.getIndicator(getTimeframe(), isLiveSeries), 70);
    }

    @Override
    public Rule getFixedExitRule(boolean isLiveSeries, Number lossPercentage, Number gainPercentage) {
        ClosePriceIndicator indicator = closePriceIndicatorM.getIndicator(getTimeframe(), isLiveSeries);
        return new StopLossRule(indicator, lossPercentage)
                .or(new StopGainRule(indicator, gainPercentage));
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

}

