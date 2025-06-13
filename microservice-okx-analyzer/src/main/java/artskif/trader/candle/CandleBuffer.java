package artskif.trader.candle;

import artskif.trader.common.Buffer;
import artskif.trader.common.LimitedLinkedHashMap;
import artskif.trader.dto.CandlestickDto;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class CandleBuffer extends Buffer<CandlestickDto> {

    public CandleBuffer(Duration interval, int maxSize) {
        super(interval, maxSize);
    }

    @Override
    public String toString() {
        return "CandleBuffer{candles=" + snapshot.values() + '}';
    }
}
