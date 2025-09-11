package artskif.trader.signal.rsi;

import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.signal.Signal;
import artskif.trader.signal.StrategyKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdxRsiSignalGenerator {

    private final BigDecimal SELL_BUY_BORDER = BigDecimal.valueOf(50);

    public List<Signal> generate(IndicatorFrame frame, StrategyKind strategyKind) {
        List<Signal> out = new ArrayList<>();
        for (IndicatorSnapshot snap : frame.indicators()) { // все индикаторы из фрейма
            if (snap.type() != IndicatorType.RSI) continue;  // берем только RSI
            if (snap.value() == null) continue;

            //SignalType type = SignalType.adx(snap.period() == null ? 0 : snap.period());

            // Пропускаем сигналы, не разрешённые для этой стратегии
//            if (!SignalRouting.allowedFor(strategyKind, type)) continue;

//            if (snap.value().compareTo(BUY_TH) < 0) {
//                out.add(new Signal(
//                        snap.bucket(),                   // время конкретного индикатора
//                        OperationType.BUY,
//                        type,
//                        frame.candleType()               // таймфрейм фрейма
//                ));
//            } else if (snap.value().compareTo(SELL_TH) > 0) {
//                out.add(new Signal(
//                        snap.bucket(),
//                        OperationType.SELL,
//                        type,
//                        frame.candleType()
//                ));
//            }
        }
        return out;
    }
}
