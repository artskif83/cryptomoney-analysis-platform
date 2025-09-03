package artskif.trader.common;

public interface Stateable {
    boolean isStateful();

    PointState getState();
}
