package artskif.trader.kafka;

import artskif.trader.candle.Candle1W;
import artskif.trader.candle.Candle1M;
import artskif.trader.candle.Candle4H;
import artskif.trader.candle.Candle5M;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Читает HISTORY-топики и передаёт пачки в тикеры через restoreFromHistory(...).
 * Сообщение — это JSON-массив "data" от харвестера (как прислал харвестер).
 */
@Startup
@ApplicationScoped
public class HistoryConsumer {

    private final static Logger LOG = Logger.getLogger(HistoryConsumer.class);

    @ConfigProperty(name = "analysis.candle1m.enabled", defaultValue = "true")
    boolean candle1mEnabled;
    @ConfigProperty(name = "analysis.candle5m.enabled", defaultValue = "true")
    boolean candle5mEnabled;
    @ConfigProperty(name = "analysis.candle4h.enabled", defaultValue = "true")
    boolean candle4hEnabled;
    @ConfigProperty(name = "analysis.candle1w.enabled", defaultValue = "true")
    boolean candle1wEnabled;

    @Inject Candle1M candle1m;
    @Inject Candle5M candle5m;
    @Inject Candle4H candle4H;
    @Inject Candle1W candle1w;

    @PostConstruct
    void init() {
        LOG.info("🔌 Старт HistoryConsumer для восстановления буферов из истории");
    }

    @Incoming("candle-1m-history")
    public void consume1mHistory(String message) {
        if (candle1mEnabled) {
            candle1m.restoreFromHistory(message);
        }
    }

    @Incoming("candle-5m-history")
    public void consume5mHistory(String message) {
        if (candle5mEnabled) {
            candle5m.restoreFromHistory(message);
        }
    }

    @Incoming("candle-4h-history")
    public void consume4hHistory(String message) {
        if (candle4hEnabled) {
            candle4H.restoreFromHistory(message);
        }
    }

    @Incoming("candle-1w-history")
    public void consume1wHistory(String message) {
        if (candle1wEnabled) {
            candle1w.restoreFromHistory(message);
        }
    }
}
