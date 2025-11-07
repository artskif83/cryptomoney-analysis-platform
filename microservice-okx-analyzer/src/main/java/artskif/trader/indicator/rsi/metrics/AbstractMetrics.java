package artskif.trader.indicator.rsi.metrics;

import artskif.trader.buffer.Buffer;
import artskif.trader.indicator.rsi.RsiPoint;

public abstract class AbstractMetrics {
    public abstract void recalculateMetric(Buffer<RsiPoint> rsiBuffer);
}
