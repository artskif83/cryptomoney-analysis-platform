package artskif.trader.kafka;

import artskif.trader.candle.Candle1D;
import artskif.trader.candle.Candle1H;
import artskif.trader.candle.Candle1m;
import artskif.trader.candle.Candle4H;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Читает HISTORY-топики и передаёт пачки в тикеры через restoreFromHistory(...).
 * Сообщение — это JSON-массив "data" от OKX (как прислал харвестер).
 */
@Startup
@ApplicationScoped
public class HistoryConsumer {

    private final static Logger LOG = Logger.getLogger(HistoryConsumer.class);

    @Inject Candle1m candle1m;
    @Inject Candle1H candle1H;
    @Inject Candle4H candle4H;
    @Inject Candle1D candle1D;

    @PostConstruct
    void init() {
        LOG.info("🔌 Старт HistoryConsumer для восстановления буферов из истории");
    }

    @Incoming("candle-1m-history")
    public void consume1mHistory(String message) {
        candle1m.restoreFromHistory(message);
    }

    @Incoming("candle-1h-history")
    public void consume1hHistory(String message) {
        candle1H.restoreFromHistory(message);
    }

    @Incoming("candle-4h-history")
    public void consume4hHistory(String message) {
        candle4H.restoreFromHistory(message);
    }

    @Incoming("candle-1d-history")
    public void consume1dHistory(String message) {
        candle1D.restoreFromHistory(message);
    }
}
