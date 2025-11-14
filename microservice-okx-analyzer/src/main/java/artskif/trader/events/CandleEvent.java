package artskif.trader.events;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;

import java.time.Instant;

public record CandleEvent(CandleTimeframe period, String instrument, Instant bucket, CandlestickDto candle, Boolean confirmed) {

    @Override
    public String toString() {
        return "CandleEvent{" + period + ", " + instrument + ", " + bucket + ", " + candle + ", " + confirmed + "}";
    }
}
