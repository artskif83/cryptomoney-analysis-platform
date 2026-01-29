package artskif.trader.kafka;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    @Inject
    Candle candle;

    @PostConstruct
    void init() {
        LOG.info("🔌 Старт HistoryConsumer для восстановления буферов из истории");
    }

    @Incoming("candle-1m-history")
    public void consume1mHistory(String message) {
        candle.restoreFromHistory(CandleTimeframe.CANDLE_1M, message);
    }

    @Incoming("candle-5m-history")
    public void consume5mHistory(String message) {
        candle.restoreFromHistory(CandleTimeframe.CANDLE_5M, message);
    }

    @Incoming("candle-1h-history")
    public void consume1hHistory(String message) {
        candle.restoreFromHistory(CandleTimeframe.CANDLE_1H, message);
    }

    @Incoming("candle-4h-history")
    public void consume4hHistory(String message) {
        candle.restoreFromHistory(CandleTimeframe.CANDLE_4H, message);
    }

    @Incoming("candle-1w-history")
    public void consume1wHistory(String message) {
        candle.restoreFromHistory(CandleTimeframe.CANDLE_1W, message);
    }
}
