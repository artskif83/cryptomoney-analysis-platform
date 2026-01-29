package artskif.trader.kafka;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@Startup
@ApplicationScoped
public class CandleConsumer {

    private final static Logger LOG = Logger.getLogger(CandleConsumer.class);

    @Inject
    Candle candle;

    @PostConstruct
    void init() {
        LOG.info("🔌 Старт консюмера для обработки свечей");
    }

    @Incoming("candle-1m")
    public void consume1m(String message) {
        candle.handleTick(CandleTimeframe.CANDLE_1M, message);
    }

    @Incoming("candle-5m")
    public void consume5m(String message) {
        candle.handleTick(CandleTimeframe.CANDLE_5M, message);
    }

    @Incoming("candle-1h")
    public void consume1h(String message) {
        candle.handleTick(CandleTimeframe.CANDLE_1H, message);
    }

    @Incoming("candle-4h")
    public void consume4H(String message) {
        candle.handleTick(CandleTimeframe.CANDLE_4H, message);
    }

    @Incoming("candle-1w")
    public void consume1W(String message) {
        candle.handleTick(CandleTimeframe.CANDLE_1W, message);
    }
}
