package artskif.trader.candle;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Единый класс для работы со всеми таймфреймами свечей.
 * Каждый таймфрейм имеет свои буферы, имя и настройки.
 */
@ApplicationScoped
public class Candle {

    private static final Logger LOG = Logger.getLogger(Candle.class);

    private final Map<CandleTimeframe, CandleInstance> instances = new EnumMap<>(CandleTimeframe.class);
    private final CandleEventBus bus;

    @ConfigProperty(name = "analysis.candle1m.enabled", defaultValue = "true")
    boolean candle1mEnabled;
    @ConfigProperty(name = "analysis.candle5m.enabled", defaultValue = "true")
    boolean candle5mEnabled;
    @ConfigProperty(name = "analysis.candle4h.enabled", defaultValue = "true")
    boolean candle4hEnabled;
    @ConfigProperty(name = "analysis.candle1w.enabled", defaultValue = "true")
    boolean candle1wEnabled;

    @Inject
    public Candle(CandleEventBus bus) {
        this.bus = bus;
    }

    @PostConstruct
    void init() {
        LOG.info("🕯️ Инициализация единого класса Candle для всех таймфреймов");

        // Инициализируем экземпляры для каждого таймфрейма, только если enabled
        if (candle1mEnabled) {
            instances.put(CandleTimeframe.CANDLE_1M, new CandleInstance(
                    CandleTimeframe.CANDLE_1M, "CANDLE-1m", candle1mEnabled, bus
            ));
        }
        if (candle5mEnabled) {
            instances.put(CandleTimeframe.CANDLE_5M, new CandleInstance(
                    CandleTimeframe.CANDLE_5M, "CANDLE-5m", candle5mEnabled, bus
            ));
        }
        if (candle4hEnabled) {
            instances.put(CandleTimeframe.CANDLE_4H, new CandleInstance(
                    CandleTimeframe.CANDLE_4H, "CANDLE-4H", candle4hEnabled, bus
            ));
        }
        if (candle1wEnabled) {
            instances.put(CandleTimeframe.CANDLE_1W, new CandleInstance(
                    CandleTimeframe.CANDLE_1W, "CANDLE-1W", candle1wEnabled, bus
            ));
        }

        // Инициализируем каждый экземпляр
        instances.values().forEach(CandleInstance::init);
    }

    /**
     * Получить экземпляр свечи для указанного таймфрейма
     */
    public CandleInstance getInstance(CandleTimeframe timeframe) {
        return instances.get(timeframe);
    }

    /**
     * Получить все экземпляры свечей
     */
    public Map<CandleTimeframe, CandleInstance> getAllInstances() {
        return instances;
    }

    /**
     * Обработать тик для указанного таймфрейма
     */
    public void handleTick(CandleTimeframe timeframe, String message) {
        CandleInstance instance = instances.get(timeframe);
        if (instance != null && instance.getEnabled()) {
            instance.handleTick(message);
        }
    }

    /**
     * Восстановить историю для указанного таймфрейма
     */
    public void restoreFromHistory(CandleTimeframe timeframe, String message) {
        CandleInstance instance = instances.get(timeframe);
        if (instance != null && instance.getEnabled()) {
            instance.restoreFromHistory(message);
        }
    }

    /**
     * Внутренний класс, представляющий экземпляр свечи для конкретного таймфрейма.
     * Содержит собственные буферы и логику обработки.
     */
    public static class CandleInstance extends AbstractCandle {

        private static final int MAX_LIVE_BUFFER_SIZE = 50;
        private static final int MAX_HISTORICAL_BUFFER_SIZE = 1000000;

        private final CandleTimeframe timeframe;
        private final String name;
        private final boolean enabled;
        private final CandleEventBus bus;
        private final Logger logger;

        private final BufferRepository<CandlestickDto> candleBufferRepository;
        private final TimeSeriesBuffer<CandlestickDto> liveBuffer;
        private final TimeSeriesBuffer<CandlestickDto> historicalBuffer;

        public CandleInstance(CandleTimeframe timeframe, String name, boolean enabled, CandleEventBus bus) {
            this.timeframe = timeframe;
            this.name = name;
            this.enabled = enabled;
            this.bus = bus;
            this.logger = Logger.getLogger(Candle.class.getName() + "." + name);

            this.liveBuffer = new TimeSeriesBuffer<>(MAX_LIVE_BUFFER_SIZE);
            this.historicalBuffer = new TimeSeriesBuffer<>(MAX_HISTORICAL_BUFFER_SIZE);
            this.candleBufferRepository = new CandleRepository();
        }

        @ActivateRequestContext
        public void init() {
            if (!enabled) {
                logger.infof("⚠️ [%s] Таймфрейм отключен", name);
                return;
            }
            logger.infof("✅ [%s] Инициализация таймфрейма", name);
            initRestoreBuffer();
        }

        @Override
        protected BufferRepository<CandlestickDto> getBufferRepository() {
            return candleBufferRepository;
        }

        @Override
        protected CandleTimeframe getCandleTimeframe() {
            return timeframe;
        }

        @Override
        public TimeSeriesBuffer<CandlestickDto> getLiveBuffer() {
            return liveBuffer;
        }

        @Override
        public TimeSeriesBuffer<CandlestickDto> getHistoricalBuffer() {
            return historicalBuffer;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Integer getMaxLiveBufferSize() {
            return MAX_LIVE_BUFFER_SIZE;
        }

        @Override
        public Integer getMaxHistoryBufferSize() {
            return MAX_HISTORICAL_BUFFER_SIZE;
        }

        @Override
        public boolean getEnabled() {
            return enabled;
        }

        @Override
        protected CandleEventBus getEventBus() {
            return bus;
        }

        @Override
        public Logger log() {
            return logger;
        }
    }
}

