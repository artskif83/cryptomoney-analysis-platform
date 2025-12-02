package artskif.trader.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

import artskif.trader.candle.CandleTimeframe;

@Data
@RegisterForReflection
public class CandlestickDto {

    private Instant timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal volumeCcy;
    private BigDecimal volumeCcyQuote;
    private Boolean confirmed;
    private Boolean saved;

    // Новые поля
    private CandleTimeframe period;
    private String instrument;
}
