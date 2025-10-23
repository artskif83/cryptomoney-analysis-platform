package artskif.trader.kafka;

import artskif.trader.candle.Candle1D;
import artskif.trader.candle.Candle1H;
import artskif.trader.candle.Candle1m;
import artskif.trader.candle.Candle4H;
import artskif.trader.indicator.rsi.RsiIndicator1m;
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
    @ConfigProperty(name = "analysis.candle1h.enabled", defaultValue = "true")
    boolean candle1hEnabled;
    @ConfigProperty(name = "analysis.candle4h.enabled", defaultValue = "true")
    boolean candle4hEnabled;
    @ConfigProperty(name = "analysis.candle1d.enabled", defaultValue = "true")
    boolean candle1dEnabled;


    @Inject
    Candle1m candle1m;
    @Inject
    Candle1H candle1H;
    @Inject
    Candle4H candle4H;
    @Inject
    Candle1D candle1D;

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

    @Incoming("candle-1h")
    public void consume1H(String message) {
        if (candle1hEnabled) {
            candle1H.handleTick(message);
        }
    }

    @Incoming("candle-4h")
    public void consume4H(String message) {
        if (candle4hEnabled) {
            candle4H.handleTick(message);
        }
    }

    @Incoming("candle-1d")
    public void consume1D(String message) {
        if (candle1dEnabled) {
            candle1D.handleTick(message);
        }
    }
}
