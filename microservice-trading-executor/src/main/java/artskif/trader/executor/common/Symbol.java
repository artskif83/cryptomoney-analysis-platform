package artskif.trader.executor.common;

public record Symbol(String base, String quote) {
    public String asPair() {
        return base + "/" + quote;
    }
}

