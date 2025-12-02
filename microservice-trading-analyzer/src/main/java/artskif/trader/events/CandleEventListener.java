package artskif.trader.events;

@FunctionalInterface
public interface CandleEventListener {
    void onCandle(CandleEvent event);
}
