package artskif.trader.indicator.rsi;

import artskif.trader.dto.CandlestickDto;

import java.time.Instant;


public record RsiPipelineContext(
        RsiState state,
        RsiPoint point,
        Instant bucket,
        CandlestickDto candle) {
}
