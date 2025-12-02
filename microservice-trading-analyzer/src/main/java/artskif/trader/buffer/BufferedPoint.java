package artskif.trader.buffer;

public interface BufferedPoint<C> {
    TimeSeriesBuffer<C> getLiveBuffer();
    TimeSeriesBuffer<C> getHistoricalBuffer();
}
