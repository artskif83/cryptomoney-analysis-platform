package artskif.trader.common;

public interface Candle<C> {
    String getName();

    Buffer<C> getBuffer();
}
