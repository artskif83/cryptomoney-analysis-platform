package artskif.trader.strategy.rsi;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.signal.rsi.AdxRsiSignalGenerator;
import artskif.trader.strategy.AbstractStrategy;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import my.signals.v1.Signal;
import my.signals.v1.StrategyKind;

import java.util.List;

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class AdxRsiStrategy extends AbstractStrategy {

    private final AdxRsiSignalGenerator generator = new AdxRsiSignalGenerator();

    @Inject
    public AdxRsiStrategy(CandleEventBus bus, List<IndicatorPoint> indicators) {
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

        Signal signal = generator.generate(frame, getStrategyKind());
        // TODO: здесь можно отправить сигналы в очередь/шину/репозиторий
        if (signal != null) System.out.println("📣 SIGNAL: " + signal);
    }

    public StrategyKind getStrategyKind() {
        return StrategyKind.ADX_RSI;
    }
}
