package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.buffer.Buffer;
import artskif.trader.common.PointState;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.indicator.rsi.metrics.AbstractMetrics;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.RsiIndicatorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@ApplicationScoped
public class RsiIndicator1m extends RsiAbstractIndicator {

    private final static String NAME = "RSI-1m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator1m.class);
    private final static Integer PERIOD = 14; // Период индикатора RSI
    private final static Integer BUFFER_SIZE = 100; // Размер буфера для хранения точек индикатора

    protected RsiIndicator1m() {
        super(null, null, null, PERIOD, new RsiIndicatorRepository(), BUFFER_SIZE);
    }

    @Inject
    public RsiIndicator1m(Candle1m candle1m, CandleEventBus bus, Instance<AbstractMetrics> metrics) {
        super(candle1m, bus, metrics, PERIOD, new RsiIndicatorRepository(), BUFFER_SIZE);
    }

    @Override
    public CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public Integer getPeriod() {
        return period;
    }

    @Override
    public Instant getBucket() {
        return bucket;
    }

    @Override
    public Instant getProcessingTime() {
        return processingTime;
    }

    @Override
    public Buffer<RsiPoint> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return String.format("%s-%dp", NAME, period);
    }

    @Override
    protected BufferRepository<RsiPoint> getBufferRepository() {
        return rsiBufferRepository;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public PointState getState() {
        return rsiState;
    }

    @Override
    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    @Override
    public BigDecimal getConfirmedValue() {
        return confirmedValue;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.RSI;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}