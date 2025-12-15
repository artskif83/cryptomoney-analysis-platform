package artskif.trader.candle;

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

    public static CandleTimeframe fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CandleTimeframe value cannot be null");
        }
        try {
            return CandleTimeframe.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CandleTimeframe: " + value +
                    ". Valid values are: CANDLE_1M, CANDLE_5M, CANDLE_4H, CANDLE_1W", e);
        }
    }
}
