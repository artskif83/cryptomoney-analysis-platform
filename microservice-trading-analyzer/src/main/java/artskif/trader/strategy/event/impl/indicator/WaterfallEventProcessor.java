package artskif.trader.strategy.event.impl.indicator;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import artskif.trader.strategy.event.TradeEventProcessor;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.regime.common.MarketRegime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.util.Optional;

/**
 * Модель событий для режима флета (бокового движения рынка)
 * Отвечает только за детектирование событий во время FLAT режима
 */
@ApplicationScoped
public class WaterfallEventProcessor implements TradeEventProcessor {

    private final RSIIndicatorM rsiIndicatorM;

    public WaterfallEventProcessor() {
        this.rsiIndicatorM = null;
    }

    @Inject
    public WaterfallEventProcessor(RSIIndicatorM rsiIndicatorM) {
        this.rsiIndicatorM = rsiIndicatorM;
    }

    @Override
    public Optional<TradeEventData> detect(MarketRegime regime) {

//        ContractSnapshot snapshot5m =
//                snapshotBuilder.build(schema5mBase, lastIndex5m, true);


        // Логика детектирования события для флетового рынка
        // Например, можно искать ложные пробои или отскоки от границ диапазона
        // TODO: Реализовать логику детектирования событий во флете

        return Optional.empty(); // Пока ничего не детектируем
    }

    @Override
    public Rule getEntryRule() {
        RSIIndicator indicator = rsiIndicatorM.getIndicator(getTimeframe(), true);
        return new CrossedUpIndicatorRule(indicator, 70);
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

}

