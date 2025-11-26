package artskif.trader.restapi.candle;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    @Override
    protected String getTimeframe() {
        return "4H";
    }

    @Override
    protected String getDbTimeframeKey() {
        return "CANDLE_4H";
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

