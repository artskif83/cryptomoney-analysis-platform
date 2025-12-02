package artskif.trader.events;

import java.time.Duration;

public enum CandleEventType {
    CANDLE_TICK("Candle Tick"),
    CANDLE_HISTORY("Historical Candle");

    private final String description;

    CandleEventType(String description) {
        this.description = description;
    }
}
