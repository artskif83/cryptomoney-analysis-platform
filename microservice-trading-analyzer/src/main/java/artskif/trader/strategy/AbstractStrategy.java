package artskif.trader.strategy;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import artskif.trader.indicator.IndicatorPoint;
import my.signals.v1.StrategyKind;

import java.util.List;


public abstract class AbstractStrategy implements CandleEventListener {

    protected abstract String getName();
    protected abstract CandleEventBus getEventBus();
    protected abstract List<IndicatorPoint> getIndicators();
    protected abstract CandleTimeframe getCandleType();
    protected abstract StrategyKind getStrategyKind();

    @Override
    public void onCandle(CandleEvent event) {
        if (event.period() != getCandleType()) return;

    }

//    /** Собираем полный срез по всем индикаторам */
//    private IndicatorFrame assembleFrame(Instant bucket, CandleTimeframe period) {
//        List<IndicatorSnapshot> snapshots = new ArrayList<>(getIndicators().size());
//
//        for (IndicatorPoint ip : getIndicators()) {
//            var value = ip.getLastPoint();
//            if (value == null) continue; // индикатор ещё не дал значение
//
//            // Красивое имя, если индикатор наследуется от AbstractIndicator
//            String name = (ip instanceof AbstractIndicator<?> ai)
//                    ? ai.getName()
//                    : (ip.getName() != null ? ip.getName() : ip.getClass().getSimpleName());
//
//            IndicatorSnapshot snap = new IndicatorSnapshot(
//                    name,
//                    ip.getType(),
//                    ip.getPeriod(),
//                    ip.getCandleTimeframe(),
//                    ip.getBucket(), // у конкретного индикатора bucket может отличаться, сохраняем его
//                    ip.getProcessingTime(),
//                    value
//            );
//            snapshots.add(snap);
//        }
//
//        return new IndicatorFrame(bucket, period, snapshots);
//    }
}
