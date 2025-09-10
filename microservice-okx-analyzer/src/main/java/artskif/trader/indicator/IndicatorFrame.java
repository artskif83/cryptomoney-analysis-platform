package artskif.trader.indicator;

import artskif.trader.candle.CandleTimeframe;

import java.time.Instant;
import java.util.List;

/** Фрейм со сводкой по всем индикаторам на один bucket */
public record IndicatorFrame(
        Instant bucket,
        CandleTimeframe candleType,
        List<IndicatorSnapshot> indicators
) {}
