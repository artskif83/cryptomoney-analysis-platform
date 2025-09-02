package artskif.trader.common;

public interface Point<C> {
    String getName();

    Buffer<C> getBuffer();
}
