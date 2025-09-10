package artskif.trader.indicator;

import artskif.trader.candle.CandleTimeframe;

import java.math.BigDecimal;
import java.time.Instant;

/** Полный снимок состояния одного индикатора в момент bucket */
public record IndicatorSnapshot(
        String name,
        IndicatorType type,
        Integer period,
        CandleTimeframe candleTimeframe,
        Instant bucket,
        BigDecimal value
) {}
