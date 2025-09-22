package artskif.trader.microserviceokxexecutor.orders.signal;

public record Symbol(String base, String quote) {
    public String asPair() { return base + "/" + quote; }
}


