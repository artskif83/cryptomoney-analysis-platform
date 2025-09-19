package artskif.trader.microserviceokxtelegrambot.orders.signal;

public record Symbol(String base, String quote) {
    public String asPair() { return base + "/" + quote; }
}


