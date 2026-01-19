package artskif.trader.events.regime;

@FunctionalInterface
public interface RegimeChangeEventListener {
    void onRegimeChange(RegimeChangeEvent event);
}

