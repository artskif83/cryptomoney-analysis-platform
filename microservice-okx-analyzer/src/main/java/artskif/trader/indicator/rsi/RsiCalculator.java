package artskif.trader.indicator.rsi;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@NoArgsConstructor
public class RsiCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int OUT_SCALE = 2;

    public static final class RsiUpdate {
        public final Optional<RsiPoint> point;
        public final RsiState state;
        public RsiUpdate(Optional<RsiPoint> point, RsiState state) {
            this.point = point;
            this.state = state;
        }
    }

    /** Предварительный расчёт RSI по текущему (неподтверждённому) close — без изменения состояния. */
    public static Optional<BigDecimal> preview(RsiState s, BigDecimal currentClose) {
        if (s == null || !s.isInitialized() || s.getLastClose() == null) return Optional.empty();

        BigDecimal delta = currentClose.subtract(s.getLastClose(), MC);
        BigDecimal gain = delta.signum() > 0 ? delta : BigDecimal.ZERO;
        BigDecimal loss = delta.signum() < 0 ? delta.abs(MC) : BigDecimal.ZERO;

        BigDecimal periodBD = BigDecimal.valueOf(s.getPeriod());
        BigDecimal tmpAvgGain = s.getAvgGain().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(gain, MC).divide(periodBD, MC);
        BigDecimal tmpAvgLoss = s.getAvgLoss().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(loss, MC).divide(periodBD, MC);

        return computeRsi(tmpAvgGain, tmpAvgLoss);
    }

    /** Коммит подтверждённой свечи: обновляем состояние и возвращаем точку. */
    public static RsiUpdate updateConfirmed(RsiState s, Instant bucket, BigDecimal close) {
        if (s == null) throw new RuntimeException("Состояние не инициализировано");

        // первый ever close — просто фиксируем lastClose (дельты нет)
        if (s.getLastClose() == null) {
            RsiState next = s.cloneWith(bucket, close, s.getSeedCount(), s.getSeedGainSum(), s.getSeedLossSum(),
                    s.getAvgGain(), s.getAvgLoss(), s.isInitialized());
            return new RsiUpdate(Optional.empty(), next);
        }

        BigDecimal delta = close.subtract(s.getLastClose(), MC);
        BigDecimal gain = delta.signum() > 0 ? delta : BigDecimal.ZERO;
        BigDecimal loss = delta.signum() < 0 ? delta.abs(MC) : BigDecimal.ZERO;

        // Ещё не инициализировались — накапливаем seed
        if (!s.isInitialized()) {
            int newSeedCount = s.getSeedCount() + 1;
            BigDecimal gainSum = s.getSeedGainSum().add(gain, MC);
            BigDecimal lossSum = s.getSeedLossSum().add(loss, MC);

            if (newSeedCount < s.getPeriod()) {
                // продолжаем накапливать, RSI пока не считаем
                RsiState next = s.cloneWith(bucket, close, newSeedCount, gainSum, lossSum,
                        s.getAvgGain(), s.getAvgLoss(), false);
                return new RsiUpdate(Optional.empty(), next);
            } else {
                // newSeedCount == period → первичная инициализация avgGain/avgLoss
                BigDecimal periodBD = BigDecimal.valueOf(s.getPeriod());
                BigDecimal avgGain = gainSum.divide(periodBD, MC);
                BigDecimal avgLoss = lossSum.divide(periodBD, MC);

                RsiState next = s.cloneWith(bucket, close, newSeedCount, gainSum, lossSum,
                        avgGain, avgLoss, true);

                Optional<BigDecimal> rsi = computeRsi(avgGain, avgLoss);
                return new RsiUpdate(rsi.map(v -> new RsiPoint(bucket, v)), next);
            }
        }

        // обычный сглаженный апдейт
        BigDecimal periodBD = BigDecimal.valueOf(s.getPeriod());
        BigDecimal newAvgGain = s.getAvgGain().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(gain, MC).divide(periodBD, MC);
        BigDecimal newAvgLoss = s.getAvgLoss().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(loss, MC).divide(periodBD, MC);

        RsiState next = s.cloneWith(bucket, close, s.getSeedCount(), s.getSeedGainSum(), s.getSeedLossSum(),
                newAvgGain, newAvgLoss, true);

        Optional<BigDecimal> rsi = computeRsi(newAvgGain, newAvgLoss);
        return new RsiUpdate(rsi.map(v -> new RsiPoint(bucket, v)), next);
    }

    private static Optional<BigDecimal> computeRsi(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgGain == null || avgLoss == null) return Optional.empty();
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return Optional.of(ONE_HUNDRED);
        if (avgGain.compareTo(BigDecimal.ZERO) == 0) return Optional.of(BigDecimal.ZERO);

        BigDecimal rs = avgGain.divide(avgLoss, MC);
        BigDecimal rsi = ONE_HUNDRED.subtract(ONE_HUNDRED.divide(BigDecimal.ONE.add(rs, MC), MC));
        return Optional.of(out(rsi));
    }

    private static BigDecimal out(BigDecimal v) { return v.setScale(OUT_SCALE, RoundingMode.HALF_UP); }


}
