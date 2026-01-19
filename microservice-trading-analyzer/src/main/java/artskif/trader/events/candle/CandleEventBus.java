package artskif.trader.events.candle;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class CandleEventBus {
    private final List<CandleEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(CandleEventListener l) { listeners.add(l); }
    public void unsubscribe(CandleEventListener l) { listeners.remove(l); }

    public void publish(CandleEvent event) {
        for (CandleEventListener l : listeners) {
            try { l.onCandle(event); } catch (Exception ignored) {}
        }
    }
}
