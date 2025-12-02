package artskif.trader.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class CandlestickPayloadDto {
    private final String channel;
    private final String instrumentId;
    private final CandlestickDto candle;

    public CandlestickPayloadDto(String channel, String instrumentId, CandlestickDto candle) {
        this.channel = channel;
        this.instrumentId = instrumentId;
        this.candle = candle;
    }
}
