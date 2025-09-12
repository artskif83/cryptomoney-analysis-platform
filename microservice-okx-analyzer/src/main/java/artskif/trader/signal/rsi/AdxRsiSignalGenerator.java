package artskif.trader.signal.rsi;

import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.signal.OperationType;
import artskif.trader.signal.Signal;
import artskif.trader.signal.StrategyKind;
import artskif.trader.signal.TrendDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AdxRsiSignalGenerator {

    private static final BigDecimal RSI_BORDER = BigDecimal.valueOf(50);

    private static final BigDecimal L25 = BigDecimal.valueOf(25);
    private static final BigDecimal L20 = BigDecimal.valueOf(20);
    private static final BigDecimal L15 = BigDecimal.valueOf(15);
    private static final List<BigDecimal> LEVELS = List.of(L25, L20, L15);

    private static final int HI_START = 30;   // старт для «высоких» уровней
    private static final int HI_STEP = 10;   // шаг 10

    // ---- антидубль на уровне бара ----
    private Instant lastSignalBucket = null;  // ★

    public Signal generate(IndicatorFrame frame, StrategyKind strategyKind) {
        // если уже подавали сигнал на этом баре — ничего не делаем
        final Instant bar = frame.bucket();                   // ★
        if (bar != null && bar.equals(lastSignalBucket)) {    // ★
            return null;
        }

        Signal out = null;

        IndicatorSnapshot adx = frame.getIndicator(IndicatorType.ADX);
        IndicatorSnapshot rsi = frame.getIndicator(IndicatorType.RSI);

        if (adx == null) return null;

        BigDecimal prev = adx.lastValue();
        BigDecimal curr = adx.value();
        if (prev == null || curr == null) return null;

        // ---------- Новая логика: уровни >= 30 каждые 10, UP и DOWN, при участии RSI > 50 продаем, меньше закупаем ----------
        if (rsi != null && rsi.value() != null) {
            boolean up = curr.compareTo(prev) > 0;
            BigDecimal lo = prev.min(curr);
            BigDecimal hi = prev.max(curr);

            int first = Math.max(ceilTo10(lo), HI_START);
            int last = floorTo10(hi);

            for (int level = first; level <= last; level += HI_STEP) {
                BigDecimal lv = BigDecimal.valueOf(level);
                boolean crossedUp = prev.compareTo(lv) < 0 && curr.compareTo(lv) >= 0;
                if (crossedUp) {
                    // возвращаем старший пересечённый уровень (идём по возрастанию, перезаписываем out)
                    out = makeSignal(frame, adx, level,
                            rsi.value().compareTo(RSI_BORDER) > 0 ? OperationType.SELL : OperationType.BUY, // оставляем BUY как и ранее
                            up ? TrendDirection.UP : TrendDirection.DOWN);
                }
            }
        }
        // ---------- Старая логика: 25/20/15, только DOWN, диапазоны как раньше ----------
        for (int i = 0; i < LEVELS.size(); i++) {
            BigDecimal level = LEVELS.get(i);
            BigDecimal lower = (i + 1 < LEVELS.size()) ? LEVELS.get(i + 1) : null;

            if (crossedDown(prev, curr, level) && (lower == null || curr.compareTo(lower) > 0)) {
                out = makeSignal(frame, adx, level.intValue(), OperationType.BUY, TrendDirection.DOWN);
                break; // старший подходящий уровень
            }
        }

        if (out != null) {
            lastSignalBucket = bar;   // ★ фиксируем, что на этот бар сигнал уже подан
        }
        return out;
    }

    private static boolean crossedDown(BigDecimal prev, BigDecimal curr, BigDecimal level) {
        return prev.compareTo(level) > 0 && curr.compareTo(level) < 0;
    }

    private static Signal makeSignal(IndicatorFrame frame, IndicatorSnapshot snap, int level, OperationType operationType, TrendDirection trendDirection) {
        return new Signal(
                snap.bucket(),
                operationType,
                StrategyKind.ADX_RSI,
                level,
                trendDirection,
                frame.candleType()
        );
    }

    /**
     * Округление вверх до ближайших 10 (29.1 -> 30, 30 -> 30)
     */
    private static int ceilTo10(BigDecimal x) {
        int v = x.intValue();
        return (v % 10 == 0) ? v : ((v / 10) + 1) * 10;
    }

    /**
     * Округление вниз до ближайших 10 (30.9 -> 30, 30 -> 30)
     */
    private static int floorTo10(BigDecimal x) {
        int v = x.intValue();
        return (v / 10) * 10;
    }
}
