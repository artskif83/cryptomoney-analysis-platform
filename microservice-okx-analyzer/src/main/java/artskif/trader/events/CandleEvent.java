package artskif.trader.events;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;

import java.time.Instant;

public record CandleEvent(CandleTimeframe period, String instrument, Instant bucket, CandlestickDto candle) {

    @Override
    public String toString() {
        return "CandleEvent{" + period + ", " + instrument + ", " + bucket + "}";
    }
}
