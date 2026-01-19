package artskif.trader.events.trade;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class TradeEventBus {
    private final List<TradeEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(TradeEventListener l) { listeners.add(l); }
    public void unsubscribe(TradeEventListener l) { listeners.remove(l); }

    public void publish(TradeEvent event) {
        for (TradeEventListener l : listeners) {
            try { l.onTrade(event); } catch (Exception ignored) {}
        }
    }
}

