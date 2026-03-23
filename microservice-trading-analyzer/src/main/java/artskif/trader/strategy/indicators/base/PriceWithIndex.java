package artskif.trader.strategy.indicators.base;

import org.ta4j.core.num.Num;

/**
 * Вспомогательный класс для хранения цены и её исходного индекса
 */
public class PriceWithIndex {
    private final Num price;
    private final int originalIndex;

    public PriceWithIndex(Num price, int originalIndex) {
        this.price = price;
        this.originalIndex = originalIndex;
    }

    public Num getPrice() {
        return price;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }

    @Override
    public String toString() {
        return "PriceWithIndex{" +
                "price=" + price +
                ", originalIndex=" + originalIndex +
                '}';
    }
}

