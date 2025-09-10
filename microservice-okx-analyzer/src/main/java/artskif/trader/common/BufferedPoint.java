package artskif.trader.common;

public interface BufferedPoint<C> {
    Buffer<C> getBuffer();
    String getName();
}
