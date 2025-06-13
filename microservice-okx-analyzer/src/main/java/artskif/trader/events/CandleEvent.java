package artskif.trader.events;

import artskif.trader.candle.CandleType;
import artskif.trader.dto.CandlestickDto;
import lombok.Getter;

import java.time.Instant;

public record CandleEvent(CandleType type, String instrument, Instant bucket, CandlestickDto candle) {

    @Override
    public String toString() {
        return "CandleEvent{" + type + ", " + instrument + ", " + bucket + "}";
    }
}
