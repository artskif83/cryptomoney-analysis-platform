package artskif.trader.candle;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CandleTimeframe {
    CANDLE_1M(Duration.ofMinutes(1)),
    CANDLE_1H(Duration.ofHours(1)),
    CANDLE_4H(Duration.ofHours(4)),
    CANDLE_1D(Duration.ofDays(1));

    private final Duration duration;

    CandleTimeframe(Duration duration) {
        this.duration = duration;
    }
}
