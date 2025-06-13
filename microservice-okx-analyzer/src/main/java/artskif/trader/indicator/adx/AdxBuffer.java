package artskif.trader.indicator.adx;

import artskif.trader.common.Buffer;

import java.time.Duration;

public class AdxBuffer extends Buffer<AdxPoint> {

    public AdxBuffer(Duration interval, int maxSize) {
        super(interval, maxSize);
    }

    @Override
    public String toString() {
        return "IndicatorBuffer{adx=" + snapshot.values() + '}';
    }
}
