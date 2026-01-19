package artskif.trader.events.candle;

import artskif.trader.candle.CandleEventType;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;

import java.time.Instant;

public record CandleEvent(CandleEventType type, CandleTimeframe period, String instrument, Instant bucket, CandlestickDto candle, Boolean confirmed, Boolean isTest) {

    @Override
    public String toString() {
        return "CandleEvent{" + type + ", " + period + ", " + instrument + ", " + bucket + ", " + candle + ", " + confirmed + ", isTest=" + isTest + "}";
    }
}
