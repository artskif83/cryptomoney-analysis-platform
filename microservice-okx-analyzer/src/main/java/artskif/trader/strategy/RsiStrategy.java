package artskif.trader.strategy;

import artskif.trader.candle.CandlePeriod;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorPoint;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import java.util.List;

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class RsiStrategy extends AbstractStrategy{

    @Inject
    public RsiStrategy(CandleEventBus bus, List<IndicatorPoint> indicators) {
        super(bus, indicators);
    }

    @Override
    protected CandlePeriod getCandleType() {
        return CandlePeriod.CANDLE_1M;
    }
}
