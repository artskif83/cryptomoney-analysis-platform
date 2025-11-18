package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle5M;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.common.Stage;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.RsiIndicatorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;


@ApplicationScoped
public class RsiIndicator5m extends RsiAbstractIndicator {

    private final static String NAME = "RSI-5m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator5m.class);
    private final static Integer PERIOD = 14; // Период индикатора RSI
    private final static Integer BUFFER_LIVE_SIZE = 100; // Размер буфера для хранения точек индикатора
    private final static Integer BUFFER_HISTORICAL_SIZE = 10000; // Размер буфера для хранения исторических точек индикатора

    protected RsiIndicator5m() {
        super(null, null, null, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Inject
    public RsiIndicator5m(Candle5M candle5m, CandleEventBus bus, Instance<Stage<RsiPipelineContext>> metrics) {
        super(candle5m, bus, metrics, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Override
    protected BufferRepository<RsiPoint> getBufferRepository() {
        return rsiBufferRepository;
    }

    @Override
    public CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_5M;
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
    public boolean isStateful() {
        return true;
    }

    @Override
    public RsiState getState() {
        return rsiState;
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

