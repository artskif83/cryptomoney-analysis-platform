package artskif.trader.repository;

import java.time.Duration;
import java.time.Instant;

/**
 * Представляет временной разрыв (гап) в последовательности свечей
 */
public class TimeGap {
    private final Instant start;
    private final Instant end;

    public TimeGap(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public Long getStartEpochMs() {
        return start == null ? null : start.toEpochMilli();
    }

    public Long getEndEpochMs() {
        return end == null ? null : end.toEpochMilli();
    }

    @Override
    public String toString() {
        String durationStr;
        if (start == null || end == null) {
            durationStr = "null";
        } else {
            durationStr = Duration.between(start, end).toString();
        }
        return String.format("TimeGap{start=%s, end=%s, duration=%s}",
                start, end, durationStr);
    }
}

