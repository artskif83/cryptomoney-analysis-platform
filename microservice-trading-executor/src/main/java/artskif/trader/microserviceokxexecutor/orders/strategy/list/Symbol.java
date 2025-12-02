package artskif.trader.microserviceokxexecutor.orders.strategy.list;

public record Symbol(String base, String quote) {
    public String asPair() { return base + "/" + quote; }

    public static Symbol fromProto(my.signals.v1.Symbol proto) {
        return new Symbol(proto.getBase(), proto.getQuote());
    }

    public my.signals.v1.Symbol toProto() {
        return my.signals.v1.Symbol.newBuilder()
                .setBase(base)
                .setQuote(quote)
                .build();
    }
}


