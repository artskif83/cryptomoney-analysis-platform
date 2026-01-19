package artskif.trader.events.candle;

@FunctionalInterface
public interface CandleEventListener {
    void onCandle(CandleEvent event);
}
