package artskif.trader.events.trade;

@FunctionalInterface
public interface TradeEventListener {
    void onTrade(TradeEvent event);
}

