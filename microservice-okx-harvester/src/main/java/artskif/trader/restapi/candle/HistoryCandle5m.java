package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Харвестер для таймфрейма 5 минут
 */
@Startup
@ApplicationScoped
public class HistoryCandle5m extends AbstractHistoryCandle {

    @ConfigProperty(name = "okx.history.5m.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "okx.history.5m.startEpochMs", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.5m.timeframe", defaultValue = "5m")
    String timeframe;

    @ConfigProperty(name = "okx.history.5m.dbTimeframeKey", defaultValue = "CANDLE_5M")
    String dbTimeframeKey;

    @Override
    protected String getTimeframe() {
        return timeframe;
    }

    @Override
    protected CandleTimeframe getTimeframeType() {
        return CandleTimeframe.CANDLE_5M;
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

