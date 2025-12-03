package artskif.trader.indicator.rsi;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.RsiPointDto;

import java.time.Instant;


public record RsiPipelineContext(
        RsiState state,
        RsiPointDto point,
        Instant bucket,
        CandlestickDto candle) {
}
