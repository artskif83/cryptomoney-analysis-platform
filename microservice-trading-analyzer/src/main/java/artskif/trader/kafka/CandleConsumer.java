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

@Startup
@ApplicationScoped
public class CandleConsumer {

    private final static Logger LOG = Logger.getLogger(CandleConsumer.class);

    @ConfigProperty(name = "analysis.candle1m.enabled", defaultValue = "true")
    boolean candle1mEnabled;
    @ConfigProperty(name = "analysis.candle5m.enabled", defaultValue = "true")
    boolean candle5mEnabled;
    @ConfigProperty(name = "analysis.candle4h.enabled", defaultValue = "true")
    boolean candle4hEnabled;
    @ConfigProperty(name = "analysis.candle1w.enabled", defaultValue = "true")
    boolean candle1wEnabled;


    @Inject
    Candle1M candle1m;
    @Inject
    Candle5M candle5m;
    @Inject
    Candle4H candle4H;
    @Inject
    Candle1W candle1w;

    @PostConstruct
    void init() {
        LOG.info("🔌 Старт консюмера для обработки свечей");
    }

    @Incoming("candle-1m")
    public void consume1m(String message) {
        if (candle1mEnabled) {
            candle1m.handleTick(message);
        }
    }

    @Incoming("candle-5m")
    public void consume5m(String message) {
        if (candle5mEnabled) {
            candle5m.handleTick(message);
        }
    }

    @Incoming("candle-4h")
    public void consume4H(String message) {
        if (candle4hEnabled) {
            candle4H.handleTick(message);
        }
    }

    @Incoming("candle-1w")
    public void consume1W(String message) {
        if (candle1wEnabled) {
            candle1w.handleTick(message);
        }
    }
}
