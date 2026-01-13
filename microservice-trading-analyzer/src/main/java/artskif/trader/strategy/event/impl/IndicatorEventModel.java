package artskif.trader.strategy.event.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.event.common.TradeEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class IndicatorEventModel implements EventModel {
    @Override
    public Optional<TradeEvent> detect(ContractSnapshot snapshot) {
        return Optional.empty();
    }

    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }
}