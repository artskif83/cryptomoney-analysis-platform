package artskif.trader.executor.orders.model;

public record Symbol(String base, String quote) {
    public String asPair() {
        return base + "/" + quote;
    }
}

