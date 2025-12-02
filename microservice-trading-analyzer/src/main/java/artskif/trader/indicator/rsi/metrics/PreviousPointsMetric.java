package artskif.trader.indicator.rsi.metrics;

import artskif.trader.common.Stage;
import artskif.trader.indicator.rsi.RsiPipelineContext;
import artskif.trader.indicator.rsi.RsiState;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PreviousPointsMetric implements Stage<RsiPipelineContext> {
    @Override
    public int order() {
        return 30;
    }

    @Override
    public RsiPipelineContext process(RsiPipelineContext input) {
        if (input.point() != null) {
            input.state().getLastNRsi().put(input.state().getTimestamp(), input.point());
        }
        if (input.candle() != null) {
            input.state().getLastNCandles().put(input.state().getTimestamp(), input.candle());
        }

        return input;
    }
}
