package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1M;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.common.Stage;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.RsiIndicatorRepository;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

@Startup
@ApplicationScoped
public class RsiIndicator1m extends RsiAbstractIndicator {

    private final static String NAME = "RSI-1m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator1m.class);
    private final static Integer PERIOD = 14; // Период индикатора RSI
    private final static Integer BUFFER_LIVE_SIZE = 100; // Размер буфера для хранения точек индикатора
    private final static Integer BUFFER_HISTORICAL_SIZE = 1000000; // Размер буфера для хранения исторических точек индикатора

    protected RsiIndicator1m() {
        super(null, null, null, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Inject
    public RsiIndicator1m(Candle1M candle1m, CandleEventBus bus, Instance<Stage<RsiPipelineContext>> metrics) {
        super(candle1m, bus, metrics, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Override
    protected BufferRepository<RsiPoint> getBufferRepository() {
        return rsiBufferRepository;
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
        return lastProcessingTime;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Integer getMaxLiveBufferSize() {
        return BUFFER_LIVE_SIZE;
    }

    @Override
    public Integer getMaxHistoryBufferSize() {
        return BUFFER_HISTORICAL_SIZE;
    }

    @Override
    public RsiPoint getLastPoint() {
        return lastPoint;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.RSI;
    }

    @Override
    public TimeSeriesBuffer<RsiPoint> getLiveBuffer() {
        return rsiLiveBuffer;
    }

    @Override
    public TimeSeriesBuffer<RsiPoint> getHistoricalBuffer() {
        return rsiHistoricalBuffer;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}


