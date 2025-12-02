package artskif.trader.common;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CandleTimeframe {
    CANDLE_1M(Duration.ofMinutes(1)),
    CANDLE_5M(Duration.ofMinutes(5)),
    CANDLE_4H(Duration.ofHours(4)),
    CANDLE_1W(Duration.ofDays(7));

    private final Duration duration;

    CandleTimeframe(Duration duration) {
        this.duration = duration;
    }
}
