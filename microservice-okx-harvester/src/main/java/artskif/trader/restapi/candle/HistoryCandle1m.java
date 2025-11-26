package artskif.trader.restapi.candle;

import io.quarkus.runtime.Startup;
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

    @Override
    protected String getTimeframe() {
        return timeframe;
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

