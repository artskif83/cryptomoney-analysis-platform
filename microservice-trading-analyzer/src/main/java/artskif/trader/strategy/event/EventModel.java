package artskif.trader.strategy.event;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.event.common.TradeEvent;

import java.util.Optional;

public interface EventModel {
    Optional<TradeEvent> detect(ContractSnapshot snapshot);

    CandleTimeframe getTimeframe();

}
