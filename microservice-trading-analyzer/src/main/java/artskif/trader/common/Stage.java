package artskif.trader.common;

public interface Stage<T> {
    int order();
    T process(T input);
}
