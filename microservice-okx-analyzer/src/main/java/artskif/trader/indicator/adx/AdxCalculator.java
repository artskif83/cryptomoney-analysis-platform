package artskif.trader.indicator.adx;

import artskif.trader.dto.CandlestickDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class AdxCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int OUT_SCALE = 2;

    public static final class AdxUpdate {
        public final Optional<AdxPoint> point;
        public final AdxState state;
        public AdxUpdate(Optional<AdxPoint> point, AdxState state) {
            this.point = point;
            this.state = state;
        }
    }

    /**
     * Подъём состояния из подтверждённой истории (миксуется в том же стиле, что RSI.tryInitFromHistory).
     * Идём по подтверждённым свечам в возрастающем порядке и вызываем updateConfirmed(...).
     * Если истории недостаточно — накапливаем seed.
     */
    public static AdxState tryInitFromHistory(AdxState s, List<Map.Entry<Instant, CandlestickDto>> lastConfirmedAsc) {
        if (s == null) throw new RuntimeException("Состояние не инициализировано");
        if (s.isInitialized()) return s;
        if (lastConfirmedAsc == null || lastConfirmedAsc.isEmpty()) return s;

        for (Map.Entry<Instant, CandlestickDto> e : lastConfirmedAsc) {
            AdxUpdate upd = updateConfirmed(s, e.getKey(), e.getValue());
            s = upd.state;
        }
        return s;
    }

    /**
     * Предпросчёт ADX для текущего (неподтверждённого) тика — без изменения состояния.
     * Делает смысл только когда ADX уже инициализирован (иначе у нас ещё нет lastAdx и смутно трактуемый DX seed).
     */
    public static Optional<BigDecimal> preview(AdxState s, CandlestickDto current) {
        if (s == null || !s.isInitialized() || s.getLastClose() == null) return Optional.empty();

        // считаем временные TR/DM и временное «следующее» сглаживание (как будто свеча подтвердилась),
        // но ничего не записываем в состояние
        Tmp tmp = stepCore(s, current.getHigh(), current.getLow(), current.getClose());
        if (tmp == null) return Optional.empty();

        // DX для «предположительно следующей» свечи
        Optional<BigDecimal> dxOpt = computeDx(tmp.diPlus, tmp.diMinus);
        if (dxOpt.isEmpty()) return Optional.empty();

        BigDecimal nextAdx = smooth(s.getLastAdx(), dxOpt.get(), s.getPeriod());
        return Optional.of(out(nextAdx));
    }

    /**
     * Коммит подтверждённой свечи: обновляем состояние и (если уже можем) возвращаем точку ADX.
     */
    public static AdxUpdate updateConfirmed(AdxState s, Instant bucket, CandlestickDto c) {
        if (s == null) throw new RuntimeException("Состояние не инициализировано");
        BigDecimal high = c.getHigh(), low = c.getLow(), close = c.getClose();

        // Первая когда-либо свеча: только фиксируем last* и timestamp
        if (s.getLastClose() == null) {
            AdxState next = s.cloneWith(
                    bucket, high, low, close,
                    s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                    s.getAtr(), s.getPlusDmSmoothed(), s.getMinusDmSmoothed(),
                    s.getDxSeedCount(), s.getDxSeedSum(),
                    s.getLastAdx(), s.isInitialized()
            );
            return new AdxUpdate(Optional.empty(), next);
        }

        // Основной шаг (TR/DM и их сглаживание)
        Tmp tmp = stepCore(s, high, low, close);
        if (tmp == null) {
            // защитно, не должно сюда падать
            AdxState next = s.cloneWith(bucket, high, low, close,
                    s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                    s.getAtr(), s.getPlusDmSmoothed(), s.getMinusDmSmoothed(),
                    s.getDxSeedCount(), s.getDxSeedSum(),
                    s.getLastAdx(), s.isInitialized());
            return new AdxUpdate(Optional.empty(), next);
        }

        // 1) Пока не добрали seedCount < period → накапливаем суммы
        if (s.getSeedCount() < s.getPeriod()) {
            int newSeed = s.getSeedCount() + 1;
            BigDecimal trSum = s.getSeedTrSum().add(tmp.tr, MC);
            BigDecimal plusSum = s.getSeedPlusDmSum().add(tmp.plusDmRaw, MC);
            BigDecimal minusSum = s.getSeedMinusDmSum().add(tmp.minusDmRaw, MC);

            if (newSeed < s.getPeriod()) {
                AdxState next = s.cloneWith(bucket, high, low, close,
                        newSeed, trSum, plusSum, minusSum,
                        s.getAtr(), s.getPlusDmSmoothed(), s.getMinusDmSmoothed(),
                        s.getDxSeedCount(), s.getDxSeedSum(),
                        s.getLastAdx(), false);
                return new AdxUpdate(Optional.empty(), next);
            } else {
                // newSeed == period → первичная инициализация ATR/+DM/-DM
                BigDecimal periodBD = BigDecimal.valueOf(s.getPeriod());
                BigDecimal atr0 = trSum.divide(periodBD, MC);
                BigDecimal plus0 = plusSum.divide(periodBD, MC);
                BigDecimal minus0 = minusSum.divide(periodBD, MC);

                // уже можно посчитать DI и первый DX (для последующего усреднения ADX)
                Optional<BigDecimal> diPlus = computeDi(plus0, atr0);
                Optional<BigDecimal> diMinus = computeDi(minus0, atr0);
                Optional<BigDecimal> dx = computeDx(diPlus.orElse(BigDecimal.ZERO), diMinus.orElse(BigDecimal.ZERO));

                int dxSeedCount = dx.isPresent() ? 1 : 0;
                BigDecimal dxSeedSum = dx.orElse(BigDecimal.ZERO);

                AdxState next = s.cloneWith(bucket, high, low, close,
                        newSeed, trSum, plusSum, minusSum,
                        atr0, plus0, minus0,
                        dxSeedCount, dxSeedSum,
                        null, /* lastAdx ещё нет */ false);

                // На самой инициализации ADX ещё не готов (нужно накопить средний DX за period),
                // поэтому точки пока не отдаём.
                return new AdxUpdate(Optional.empty(), next);
            }
        }

        // 2) Сглаживание после инициализации ATR/+DM/-DM
        BigDecimal periodBD = BigDecimal.valueOf(s.getPeriod());
        BigDecimal atr = s.getAtr().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(tmp.tr, MC).divide(periodBD, MC);
        BigDecimal plusDm = s.getPlusDmSmoothed().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(tmp.plusDmRaw, MC).divide(periodBD, MC);
        BigDecimal minusDm = s.getMinusDmSmoothed().multiply(periodBD.subtract(BigDecimal.ONE, MC), MC)
                .add(tmp.minusDmRaw, MC).divide(periodBD, MC);

        Optional<BigDecimal> diPlus = computeDi(plusDm, atr);
        Optional<BigDecimal> diMinus = computeDi(minusDm, atr);
        Optional<BigDecimal> dxOpt = computeDx(diPlus.orElse(BigDecimal.ZERO), diMinus.orElse(BigDecimal.ZERO));

        // 2a) Если ADX ещё не инициализирован — усредняем DX ещё period раз
        if (!s.isInitialized()) {
            int dxCount = s.getDxSeedCount() + (dxOpt.isPresent() ? 1 : 0);
            BigDecimal dxSum = s.getDxSeedSum().add(dxOpt.orElse(BigDecimal.ZERO), MC);

            if (dxCount < s.getPeriod()) {
                AdxState next = s.cloneWith(bucket, high, low, close,
                        s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                        atr, plusDm, minusDm,
                        dxCount, dxSum,
                        null, false);
                return new AdxUpdate(Optional.empty(), next);
            } else {
                // dxCount == period → первичный ADX = средний DX
                BigDecimal adx0 = dxSum.divide(periodBD, MC);
                BigDecimal outAdx = out(adx0);
                AdxState next = s.cloneWith(bucket, high, low, close,
                        s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                        atr, plusDm, minusDm,
                        dxCount, dxSum,
                        adx0, true);

                return new AdxUpdate(Optional.of(new AdxPoint(bucket, outAdx)), next);
            }
        }

        // 2b) Обычный рабочий шаг: ADX_t = (ADX_{t-1}*(n-1) + DX_t)/n
        if (dxOpt.isEmpty()) {
            // редкая защита от деления на 0 → точки нет, состояние обновили
            AdxState next = s.cloneWith(bucket, high, low, close,
                    s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                    atr, plusDm, minusDm,
                    s.getDxSeedCount(), s.getDxSeedSum(),
                    s.getLastAdx(), s.isInitialized());
            return new AdxUpdate(Optional.empty(), next);
        }

        BigDecimal nextAdx = smooth(s.getLastAdx(), dxOpt.get(), s.getPeriod());
        BigDecimal outAdx = out(nextAdx);

        AdxState next = s.cloneWith(bucket, high, low, close,
                s.getSeedCount(), s.getSeedTrSum(), s.getSeedPlusDmSum(), s.getSeedMinusDmSum(),
                atr, plusDm, minusDm,
                s.getDxSeedCount(), s.getDxSeedSum(),
                nextAdx, true);

        return new AdxUpdate(Optional.of(new AdxPoint(bucket, outAdx)), next);
    }

    // ====== математика шага ======

    private static final class Tmp {
        final BigDecimal tr, plusDmRaw, minusDmRaw, diPlus, diMinus;
        Tmp(BigDecimal tr, BigDecimal plusDmRaw, BigDecimal minusDmRaw, BigDecimal diPlus, BigDecimal diMinus) {
            this.tr = tr; this.plusDmRaw = plusDmRaw; this.minusDmRaw = minusDmRaw;
            this.diPlus = diPlus; this.diMinus = diMinus;
        }
    }

    /** Вычисляет TR/+DM/-DM и (если возможно) DI+/DI- для текущего бара, исходя из состояния s. */
    private static Tmp stepCore(AdxState s, BigDecimal high, BigDecimal low, BigDecimal close) {
        if (s.getLastHigh() == null || s.getLastLow() == null || s.getLastClose() == null) return null;

        BigDecimal tr = max3(
                high.subtract(low, MC).abs(MC),
                high.subtract(s.getLastClose(), MC).abs(MC),
                low.subtract(s.getLastClose(), MC).abs(MC)
        );

        BigDecimal upMove = high.subtract(s.getLastHigh(), MC);
        BigDecimal downMove = s.getLastLow().subtract(low, MC);

        BigDecimal plusDmRaw = (upMove.signum() > 0 && upMove.compareTo(downMove) > 0) ? upMove : BigDecimal.ZERO;
        BigDecimal minusDmRaw = (downMove.signum() > 0 && downMove.compareTo(upMove) > 0) ? downMove : BigDecimal.ZERO;

        // Если s уже имеет сглаженные значения — можем сразу прикинуть DI для превью;
        // при первичном seed возвращаем DI=0 (их всё равно пересчитаем при commit)
        BigDecimal diPlus = BigDecimal.ZERO;
        BigDecimal diMinus = BigDecimal.ZERO;
        if (s.getAtr() != null && s.getPlusDmSmoothed() != null && s.getMinusDmSmoothed() != null) {
            // «временные» следующие сглаженные значения, как будто мы их уже обновили
            BigDecimal n = BigDecimal.valueOf(s.getPeriod());
            BigDecimal atrTmp = s.getAtr().multiply(n.subtract(BigDecimal.ONE, MC), MC).add(tr, MC).divide(n, MC);
            BigDecimal plusDmTmp = s.getPlusDmSmoothed().multiply(n.subtract(BigDecimal.ONE, MC), MC).add(plusDmRaw, MC).divide(n, MC);
            BigDecimal minusDmTmp = s.getMinusDmSmoothed().multiply(n.subtract(BigDecimal.ONE, MC), MC).add(minusDmRaw, MC).divide(n, MC);

            diPlus = computeDi(plusDmTmp, atrTmp).orElse(BigDecimal.ZERO);
            diMinus = computeDi(minusDmTmp, atrTmp).orElse(BigDecimal.ZERO);
        }

        return new Tmp(tr, plusDmRaw, minusDmRaw, diPlus, diMinus);
    }

    private static Optional<BigDecimal> computeDi(BigDecimal dmSmoothed, BigDecimal atr) {
        if (dmSmoothed == null || atr == null) return Optional.empty();
        if (atr.compareTo(BigDecimal.ZERO) == 0) return Optional.of(BigDecimal.ZERO);
        return Optional.of(ONE_HUNDRED.multiply(dmSmoothed.divide(atr, MC), MC));
    }

    private static Optional<BigDecimal> computeDx(BigDecimal diPlus, BigDecimal diMinus) {
        BigDecimal sum = diPlus.add(diMinus, MC);
        if (sum.compareTo(BigDecimal.ZERO) == 0) return Optional.of(BigDecimal.ZERO);
        BigDecimal diff = diPlus.subtract(diMinus, MC).abs(MC);
        return Optional.of(ONE_HUNDRED.multiply(diff.divide(sum, MC), MC));
    }

    private static BigDecimal smooth(BigDecimal prev, BigDecimal cur, int period) {
        BigDecimal n = BigDecimal.valueOf(period);
        return (prev == null)
                ? cur
                : prev.multiply(n.subtract(BigDecimal.ONE, MC), MC).add(cur, MC).divide(n, MC);
    }

    private static BigDecimal max3(BigDecimal a, BigDecimal b, BigDecimal c) {
        BigDecimal m = a.max(b);
        return m.max(c);
    }

    private static BigDecimal out(BigDecimal v) { return v.setScale(OUT_SCALE, RoundingMode.HALF_UP); }
}
