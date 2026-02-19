package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Харвестер для таймфрейма 1 неделя
 */
@Startup
@ApplicationScoped
public class HistoryCandle1w extends AbstractHistoryCandle {

    @ConfigProperty(name = "okx.history.1w.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "okx.history.1w.startEpochMs", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.1w.timeframe", defaultValue = "1W")
    String timeframe;

    @ConfigProperty(name = "okx.history.1w.dbTimeframeKey", defaultValue = "CANDLE_1W")
    String dbTimeframeKey;

    /**
     * Метод запускается по расписанию в 30-ю секунду каждую неделю (понедельник 00:00:30).
     * Вызывает асинхронную синхронизацию данных.
     */
    @Scheduled(cron = "30 0 0 ? * MON", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledSync() {
        syncScheduled();
    }

    @Override
    protected String getTimeframe() {
        return timeframe;
    }

    @Override
    protected CandleTimeframe getTimeframeType() {
        return CandleTimeframe.CANDLE_1W;
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

