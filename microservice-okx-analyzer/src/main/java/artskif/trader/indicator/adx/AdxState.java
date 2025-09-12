package artskif.trader.indicator.adx;

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
public class AdxState implements PointState {

    private Integer period;

    // отметка времени последней подтверждённой свечи, на которой обновляли состояние
    private Instant timestamp;

    // последние цены для расчётов TR/DM
    private BigDecimal lastHigh;
    private BigDecimal lastLow;
    private BigDecimal lastClose;

    // seed-накопление для первичного ATR / +DM / -DM
    private int seedCount;                // сколько подтверждённых свечей накоплено для сглаживания
    private BigDecimal seedTrSum;         // сумма TR за seed
    private BigDecimal seedPlusDmSum;     // сумма +DM за seed
    private BigDecimal seedMinusDmSum;    // сумма -DM за seed

    // сглаженные значения по Уайлдеру (после seed)
    private BigDecimal atr;               // smoothed TR
    private BigDecimal plusDmSmoothed;    // smoothed +DM
    private BigDecimal minusDmSmoothed;   // smoothed -DM

    // инициализация самого ADX требует усреднения DX ещё за period
    private int dxSeedCount;              // счётчик DX для первичного ADX
    private BigDecimal dxSeedSum;         // сумма DX для первичного ADX

    private BigDecimal lastAdx;           // последнее значение ADX (если уже инициализирован)
    private boolean initialized;          // готов ли ADX (а не только ATR/DM)

    public static AdxState empty(int period) {
        return new AdxState(
                period,
                null,
                null, null, null,
                0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null,
                0, BigDecimal.ZERO,
                null,
                false
        );
    }

    public boolean isInitialized() { return initialized; }

    @Override
    public void restoreObject(PointState state) {
        if (state == null) {
            return;
        }
        var restoreState = (AdxState) state;

        this.period = restoreState.getPeriod();
        this.timestamp = restoreState.getTimestamp();

        this.lastHigh = restoreState.getLastHigh();
        this.lastLow = restoreState.getLastLow();
        this.lastClose = restoreState.getLastClose();

        this.seedCount = restoreState.getSeedCount();
        this.seedTrSum = restoreState.getSeedTrSum();
        this.seedPlusDmSum = restoreState.getSeedPlusDmSum();
        this.seedMinusDmSum = restoreState.getSeedMinusDmSum();

        this.atr = restoreState.getAtr();
        this.plusDmSmoothed = restoreState.getPlusDmSmoothed();
        this.minusDmSmoothed = restoreState.getMinusDmSmoothed();

        this.dxSeedCount = restoreState.getDxSeedCount();
        this.dxSeedSum = restoreState.getDxSeedSum();

        this.lastAdx = restoreState.getLastAdx();
        this.initialized = restoreState.isInitialized();
    }

    public AdxState cloneWith(
            Instant timestamp,
            BigDecimal lastHigh,
            BigDecimal lastLow,
            BigDecimal lastClose,
            int seedCount,
            BigDecimal seedTrSum,
            BigDecimal seedPlusDmSum,
            BigDecimal seedMinusDmSum,
            BigDecimal atr,
            BigDecimal plusDmSmoothed,
            BigDecimal minusDmSmoothed,
            int dxSeedCount,
            BigDecimal dxSeedSum,
            BigDecimal lastAdx,
            boolean initialized
    ) {
        return new AdxState(
                this.period,
                timestamp,
                lastHigh, lastLow, lastClose,
                seedCount,
                seedTrSum, seedPlusDmSum, seedMinusDmSum,
                atr, plusDmSmoothed, minusDmSmoothed,
                dxSeedCount, dxSeedSum,
                lastAdx,
                initialized
        );
    }
}
