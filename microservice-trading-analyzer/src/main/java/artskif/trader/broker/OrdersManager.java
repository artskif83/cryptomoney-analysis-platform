package artskif.trader.broker;

import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
@NoArgsConstructor
public class OrdersManager extends AbstractOrdersManager {

    private static final Logger log = LoggerFactory.getLogger(OrdersManager.class);

    @Inject
    public OrdersManager(CandleEventBus candleEventBus) {
        super(candleEventBus);
    }

    @Override
    protected void handleCandleEvent(CandleEvent event) {
        // TODO: реализовать логику управления ордерами
        log.debug("🔄 [stub] handleCandleEvent: {}", event);
    }
}
