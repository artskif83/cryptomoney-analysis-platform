package artskif.trader.indicator.rsi;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.common.PointState;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class RsiState implements PointState {
    private Instant timestamp;     // время состояния

    private int period;                 // окно RSI (обычно 14)
    private CandleTimeframe timeframe;  // таймфрейм свечей

    // для инициализации (seed) — накапливаем суммы приростов/потерь за period дельт
    private int seedCount;              // сколько дельт уже накопили (0..period)
    private BigDecimal seedGainSum;     // сумма gains за seed-окно
    private BigDecimal seedLossSum;     // сумма losses за seed-окно

    // сглаженные средние по Уайлдеру после инициализации
    private BigDecimal avgGain;
    private BigDecimal avgLoss;

    // предыдущий подтверждённый close
    private BigDecimal lastClose;

    // признак, что avgGain/avgLoss инициализированы
    private boolean initialized;

    public static RsiState empty(int period, CandleTimeframe timeframe) {
        return new RsiState(
                null,
                period,
                timeframe,
                0,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                null,
                false
        );
    }

    @Override
    public void restoreObject(PointState state) {
        if (state == null) {return;}
        var restoreState = (RsiState) state;
        this.period = restoreState.getPeriod();
        this.timeframe = restoreState.getTimeframe();
        this.setSeedCount(restoreState.getSeedCount());
        this.setTimestamp(restoreState.getTimestamp());
        this.setSeedGainSum(restoreState.getSeedGainSum());
        this.setSeedLossSum(restoreState.getSeedLossSum());
        this.setAvgGain(restoreState.getAvgGain());
        this.setAvgLoss(restoreState.getAvgLoss());
        this.setLastClose(restoreState.getLastClose());
        this.setInitialized(restoreState.isInitialized());
    }

    public RsiState cloneWith(Instant bucket, BigDecimal lastClose,
                                     int seedCount, BigDecimal seedGainSum, BigDecimal seedLossSum,
                                     BigDecimal avgGain, BigDecimal avgLoss,
                                     boolean initialized) {
        RsiState n = new RsiState();
        n.setPeriod(this.getPeriod());
        n.setTimeframe(this.getTimeframe());
        n.setTimestamp(bucket);
        n.setSeedCount(seedCount);
        n.setSeedGainSum(seedGainSum);
        n.setSeedLossSum(seedLossSum);
        n.setAvgGain(avgGain);
        n.setAvgLoss(avgLoss);
        n.setLastClose(lastClose);
        n.setInitialized(initialized);
        return n;
    }
}
