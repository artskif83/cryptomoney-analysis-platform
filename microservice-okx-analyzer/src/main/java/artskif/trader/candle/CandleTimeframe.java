package artskif.trader.candle;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CandleTimeframe {
    CANDLE_1M(Duration.ofMinutes(1), Duration.ofSeconds(5)),
    CANDLE_1H(Duration.ofHours(1), Duration.ofSeconds(30)),
    CANDLE_4H(Duration.ofHours(4), Duration.ofMinutes(2)),
    CANDLE_1D(Duration.ofDays(1), Duration.ofMinutes(5));

    private final Duration duration;
    private final Duration acceptableTimeMargin;

    CandleTimeframe(Duration duration, Duration acceptableTimeMargin) {
        this.duration = duration;
        this.acceptableTimeMargin = acceptableTimeMargin;
    }
}
