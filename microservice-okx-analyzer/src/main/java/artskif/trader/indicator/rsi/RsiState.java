package artskif.trader.indicator.rsi;

import artskif.trader.common.PointState;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class RsiState implements PointState {
    private int period;                 // окно RSI (обычно 14)

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

    public static RsiState empty(int period) {
        return new RsiState(
                period,
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
        this.setSeedCount(restoreState.getSeedCount());
        this.setSeedGainSum(restoreState.getSeedGainSum());
        this.setSeedLossSum(restoreState.getSeedLossSum());
        this.setAvgGain(restoreState.getAvgGain());
        this.setAvgLoss(restoreState.getAvgLoss());
        this.setLastClose(restoreState.getLastClose());
        this.setInitialized(restoreState.isInitialized());
    }

    public RsiState cloneWith(BigDecimal lastClose,
                                     int seedCount, BigDecimal seedGainSum, BigDecimal seedLossSum,
                                     BigDecimal avgGain, BigDecimal avgLoss,
                                     boolean initialized) {
        RsiState n = new RsiState();
        n.setPeriod(this.getPeriod());
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
