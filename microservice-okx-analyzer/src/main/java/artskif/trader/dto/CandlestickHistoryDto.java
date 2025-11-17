package artskif.trader.dto;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * DTO для исторических данных свечей в формате {instId, isLast, data}
 */
@Getter
@ToString
public class CandlestickHistoryDto {
    private final String instId;
    private final boolean isLast;
    private final Map<Instant, CandlestickDto> data;

    public CandlestickHistoryDto(String instId, boolean isLast, Map<Instant, CandlestickDto> data) {
        this.instId = instId;
        this.isLast = isLast;
        this.data = data;
    }
}

