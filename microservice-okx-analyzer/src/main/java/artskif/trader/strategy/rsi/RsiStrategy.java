package artskif.trader.strategy.rsi;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.signal.Signal;
import artskif.trader.signal.StrategyKind;
import artskif.trader.signal.rsi.RsiSignalGenerator;
import artskif.trader.strategy.AbstractStrategy;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import java.util.List;

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class RsiStrategy extends AbstractStrategy {

    private final RsiSignalGenerator generator = new RsiSignalGenerator();

    @Inject
    public RsiStrategy(CandleEventBus bus, List<IndicatorPoint> indicators) {
        super(bus, indicators);
    }

    @Override
    protected CandleTimeframe getCandleType() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public void onCandle(CandleEvent event) {
        super.onCandle(event); // соберёт IndicatorFrame и положит в lastFrame
        var frame = getLastFrame();
        if (frame == null) return;

        List<Signal> signals = generator.generate(frame, getStrategyKind());
        // TODO: здесь можно отправить сигналы в очередь/шину/репозиторий
        signals.forEach(s -> System.out.println("📣 SIGNAL: " + s));
    }

    public StrategyKind getStrategyKind() {
        return StrategyKind.TRIPLE_RSI;
    }
}
