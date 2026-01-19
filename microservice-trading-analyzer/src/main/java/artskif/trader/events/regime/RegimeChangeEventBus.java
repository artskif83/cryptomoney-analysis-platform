package artskif.trader.events.regime;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class RegimeChangeEventBus {
    private final List<RegimeChangeEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(RegimeChangeEventListener l) { listeners.add(l); }
    public void unsubscribe(RegimeChangeEventListener l) { listeners.remove(l); }

    public void publish(RegimeChangeEvent event) {
        for (RegimeChangeEventListener l : listeners) {
            try { l.onRegimeChange(event); } catch (Exception ignored) {}
        }
    }
}

