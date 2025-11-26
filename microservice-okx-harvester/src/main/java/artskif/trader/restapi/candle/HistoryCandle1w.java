package artskif.trader.restapi.candle;

import io.quarkus.runtime.Startup;
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

