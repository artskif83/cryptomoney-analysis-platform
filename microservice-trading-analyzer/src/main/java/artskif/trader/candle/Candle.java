package artskif.trader.candle;

import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.repository.CandleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
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
    private final CandleRepository candleRepository;

    @ConfigProperty(name = "analysis.candle1m.enabled", defaultValue = "true")
    boolean candle1mEnabled;
    @ConfigProperty(name = "analysis.candle1m.max-live-buffer-size", defaultValue = "10000")
    int candle1mMaxLiveBufferSize;
    @ConfigProperty(name = "analysis.candle1m.max-historical-buffer-size", defaultValue = "1000000")
    int candle1mMaxHistoricalBufferSize;

    @ConfigProperty(name = "analysis.candle5m.enabled", defaultValue = "true")
    boolean candle5mEnabled;
    @ConfigProperty(name = "analysis.candle5m.max-live-buffer-size", defaultValue = "10000")
    int candle5mMaxLiveBufferSize;
    @ConfigProperty(name = "analysis.candle5m.max-historical-buffer-size", defaultValue = "1000000")
    int candle5mMaxHistoricalBufferSize;

    @ConfigProperty(name = "analysis.candle4h.enabled", defaultValue = "true")
    boolean candle4hEnabled;
    @ConfigProperty(name = "analysis.candle4h.max-live-buffer-size", defaultValue = "10000")
    int candle4hMaxLiveBufferSize;
    @ConfigProperty(name = "analysis.candle4h.max-historical-buffer-size", defaultValue = "1000000")
    int candle4hMaxHistoricalBufferSize;

    @ConfigProperty(name = "analysis.candle1w.enabled", defaultValue = "true")
    boolean candle1wEnabled;
    @ConfigProperty(name = "analysis.candle1w.max-live-buffer-size", defaultValue = "10000")
    int candle1wMaxLiveBufferSize;
    @ConfigProperty(name = "analysis.candle1w.max-historical-buffer-size", defaultValue = "1000000")
    int candle1wMaxHistoricalBufferSize;

    @Inject
    public Candle(CandleEventBus bus, CandleRepository candleRepository) {
        this.bus = bus;
        this.candleRepository = candleRepository;
    }

    @PostConstruct
    void init() {
        LOG.info("🔌 Инициализация единого класса Candle для всех таймфреймов");

        // Инициализируем экземпляры для каждого таймфрейма, только если enabled
        if (candle1mEnabled) {
            instances.put(CandleTimeframe.CANDLE_1M, new CandleInstance(
                    CandleTimeframe.CANDLE_1M, "CANDLE-1m",
                    candle1mMaxLiveBufferSize, candle1mMaxHistoricalBufferSize, bus, candleRepository
            ));
        }
        if (candle5mEnabled) {
            instances.put(CandleTimeframe.CANDLE_5M, new CandleInstance(
                    CandleTimeframe.CANDLE_5M, "CANDLE-5m",
                    candle5mMaxLiveBufferSize, candle5mMaxHistoricalBufferSize, bus, candleRepository
            ));
        }
        if (candle4hEnabled) {
            instances.put(CandleTimeframe.CANDLE_4H, new CandleInstance(
                    CandleTimeframe.CANDLE_4H, "CANDLE-4H",
                    candle4hMaxLiveBufferSize, candle4hMaxHistoricalBufferSize, bus, candleRepository
            ));
        }
        if (candle1wEnabled) {
            instances.put(CandleTimeframe.CANDLE_1W, new CandleInstance(
                    CandleTimeframe.CANDLE_1W, "CANDLE-1W",
                    candle1wMaxLiveBufferSize, candle1wMaxHistoricalBufferSize, bus, candleRepository
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
        if (instance != null) {
            instance.handleTick(message);
        }
    }

    /**
     * Восстановить историю для указанного таймфрейма
     */
    public void restoreFromHistory(CandleTimeframe timeframe, String message) {
        CandleInstance instance = instances.get(timeframe);
        if (instance != null) {
            instance.restoreFromHistory(message);
        }
    }
}

