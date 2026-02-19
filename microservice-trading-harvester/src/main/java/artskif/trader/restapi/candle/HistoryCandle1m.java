package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Харвестер для таймфрейма 1 минута
 */
@Startup
@ApplicationScoped
public class HistoryCandle1m extends AbstractHistoryCandle {

    @ConfigProperty(name = "okx.history.1m.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "okx.history.1m.startEpochMs", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.1m.timeframe", defaultValue = "1m")
    String timeframe;

    @ConfigProperty(name = "okx.history.1m.dbTimeframeKey", defaultValue = "CANDLE_1M")
    String dbTimeframeKey;

    /**
     * Метод запускается по расписанию в 30-ю секунду каждой минуты.
     * Вызывает асинхронную синхронизацию данных.
     */
    @Scheduled(cron = "30 * * * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scheduledSync() {
        syncScheduled();
    }

    @Override
    protected String getTimeframe() {
        return timeframe;
    }

    @Override
    protected CandleTimeframe getTimeframeType() {
        return CandleTimeframe.CANDLE_1M;
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

