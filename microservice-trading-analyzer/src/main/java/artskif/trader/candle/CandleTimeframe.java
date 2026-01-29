package artskif.trader.candle;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CandleTimeframe {
    CANDLE_1M(Duration.ofMinutes(1), "1m"),
    CANDLE_5M(Duration.ofMinutes(5), "5m"),
    CANDLE_1H(Duration.ofHours(1), "1h"),
    CANDLE_4H(Duration.ofHours(4), "4h"),
    CANDLE_1W(Duration.ofDays(7), "1w");

    private final Duration duration;
    private final String shortName;

    CandleTimeframe(Duration duration, String shortName) {
        this.duration = duration;
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    public static CandleTimeframe fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CandleTimeframe value cannot be null");
        }
        try {
            return CandleTimeframe.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CandleTimeframe: " + value +
                    ". Valid values are: CANDLE_1M, CANDLE_5M, CANDLE_1H, CANDLE_4H, CANDLE_1W", e);
        }
    }
}
