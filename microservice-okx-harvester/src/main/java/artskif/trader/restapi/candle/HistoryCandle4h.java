package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.TimeUnit;

/**
 * Харвестер для таймфрейма 4 часа
 */
@Startup
@ApplicationScoped
public class HistoryCandle4h extends AbstractHistoryCandle {

    @ConfigProperty(name = "okx.history.4h.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "okx.history.4h.startEpochMs", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.4h.timeframe", defaultValue = "4H")
    String timeframe;

    @ConfigProperty(name = "okx.history.4h.dbTimeframeKey", defaultValue = "CANDLE_4H")
    String dbTimeframeKey;

    /**
     * Метод запускается по расписанию каждые syncIntervalSeconds секунд.
     * Вызывает асинхронную синхронизацию данных.
     */
    @Scheduled(delay = 5, delayUnit = TimeUnit.SECONDS, every = "${okx.history.4h.syncIntervalSeconds}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledSync() {
        syncScheduled();
    }

    @Override
    protected String getTimeframe() {
        return timeframe;
    }

    @Override
    protected CandleTimeframe getTimeframeType() {
        return CandleTimeframe.CANDLE_4H;
    }

    @Override
    protected String getDbTimeframeKey() {
        return dbTimeframeKey;
    }

    @Override
    protected boolean isEnabled() {
        return enabled;
    }

    @Override
    protected long getStartEpochMs() {
        return startEpochMs;
    }
}

