package artskif.trader.indicator.rsi;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.common.Stage;
import artskif.trader.dto.RsiPointDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.RsiIndicatorRepository;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;

@Startup
@ApplicationScoped
public class RsiIndicator5m extends RsiAbstractIndicator {

    private final static String NAME = "RSI-5m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator5m.class);
    private final static Integer PERIOD = 14; // Период индикатора RSI
    private final static Integer BUFFER_LIVE_SIZE = 50; // Размер буфера для хранения точек индикатора
    private final static Integer BUFFER_HISTORICAL_SIZE = 1000000; // Размер буфера для хранения исторических точек индикатора

    @ConfigProperty(name = "analysis.candle5m.enabled", defaultValue = "true")
    boolean enabled;

    protected RsiIndicator5m() {
        super(null, null, null, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Inject
    public RsiIndicator5m(Candle candle, CandleEventBus bus, Instance<Stage<RsiPipelineContext>> metrics) {
        super(candle.getInstance(CandleTimeframe.CANDLE_5M), bus, metrics, PERIOD, new RsiIndicatorRepository(), BUFFER_LIVE_SIZE, BUFFER_HISTORICAL_SIZE);
    }

    @Override
    protected BufferRepository<RsiPointDto> getBufferRepository() {
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
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.RSI;
    }

    @Override
    public TimeSeriesBuffer<RsiPointDto> getLiveBuffer() {
        return rsiLiveBuffer;
    }

    @Override
    public TimeSeriesBuffer<RsiPointDto> getHistoricalBuffer() {
        return rsiHistoricalBuffer;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}


