package strategy;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.strategy.rsi.OneHourRsiStrategy;
import my.signals.v1.OperationType;
import my.signals.v1.Signal;
import my.signals.v1.SignalLevel;
import my.signals.v1.StrategyKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для DualRsiStrategy (RSI 1h + RSI 1d).
 * Тестируем:
 *  - BUY/SELL условия
 *  - уровни по дневному RSI
 *  - запрет новых сигналов до пересечения 50 на H1
 *  - антидубль по одному и тому же H1-бару
 *  - первый вызов без lastValue
 */
class DualRsiStrategyTest {

    private OneHourRsiStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new OneHourRsiStrategy();
    }

    // --------- фабрики снапшотов (используем сам record IndicatorSnapshot) ---------
    private static IndicatorSnapshot h1(double prev, double curr, String isoBucket) {
        Instant b = Instant.parse(isoBucket);
        return new IndicatorSnapshot(
                "RSI_H1",
                IndicatorType.RSI,
                14, // период, подставь свой при необходимости
                CandleTimeframe.CANDLE_1H,
                b,  // bucket
                b,  // ts
                BigDecimal.valueOf(prev),
                BigDecimal.valueOf(curr)
        );
    }

    private static IndicatorSnapshot h1NoPrev(double curr, String isoBucket) {
        Instant b = Instant.parse(isoBucket);
        return new IndicatorSnapshot(
                "RSI_H1",
                IndicatorType.RSI,
                14,
                CandleTimeframe.CANDLE_1H,
                b, b,
                null,                                // lastValue = null
                BigDecimal.valueOf(curr)
        );
    }

    private static IndicatorSnapshot d1(Double curr, String isoBucket) {
        Instant b = Instant.parse(isoBucket);
        return new IndicatorSnapshot(
                "RSI_D1",
                IndicatorType.RSI,
                14,
                CandleTimeframe.CANDLE_1D,
                b, b,
                null,                                // lastValue нам не нужен
                curr == null ? null : BigDecimal.valueOf(curr)
        );
    }

    // --------------------------- тесты ---------------------------

    @Test
    void buy_whenDailyBelow50_andHourlyCrosses30Up() {
        Signal s = strategy.generate(
                h1(29, 31, "2025-01-01T10:00:00Z"),      // пересечение 30 снизу вверх
                d1(45.0, "2025-01-01T00:00:00Z"),         // дневной < 50
                BigDecimal.valueOf(10000),
                StrategyKind.RSI_DUAL_TF
        );
        assertNotNull(s);
        assertEquals(OperationType.BUY, s.getOperation());
        assertEquals(SignalLevel.SMALL, s.getLevel());      // 40..50 -> SMALL
        // если в .proto price=double:
        assertEquals(10000.0, s.getPrice(), 1e-9);
    }

    @Test
    void sell_whenDailyAbove60_andHourlyCrosses70Down() {
        Signal s = strategy.generate(
                h1(71, 69, "2025-01-01T11:00:00Z"),       // пересечение 70 сверху вниз
                d1(65.0, "2025-01-01T00:00:00Z"),         // 60..70 -> MIDDLE
                BigDecimal.valueOf(12345.67),
                StrategyKind.RSI_DUAL_TF
        );
        assertNotNull(s);
        assertEquals(OperationType.SELL, s.getOperation());
        assertEquals(SignalLevel.MIDDLE, s.getLevel());
    }

    @Test
    void noNewSignals_untilResetAt50_thenAllowedAgain() {
        // 1) Сгенерили BUY → блок включился
        assertNotNull(strategy.generate(
                h1(29, 31, "2025-01-01T12:00:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));

        // 2) Пока 50 не пересекли — новых сигналов нет
        assertNull(strategy.generate(
                h1(40, 35, "2025-01-01T12:01:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));

        // 3) Пересечение 50 (в любую сторону) — блок снимается, но на этом баре сигнал не генерим
        assertNull(strategy.generate(
                h1(49, 51, "2025-01-01T12:02:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));

        // 4) Теперь снова можно: новый BUY при пересечении 30↑
        assertNotNull(strategy.generate(
                h1(29, 31, "2025-01-01T12:03:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));
    }

    @Test
    void antiDuplicate_onSameH1Bar() {
        Instant sameBar = Instant.parse("2025-01-01T14:00:00Z");

        assertNotNull(strategy.generate(
                h1(29, 31, sameBar.toString()),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));

        // На том же баре второй сигнал не должен генериться
        assertNull(strategy.generate(
                h1(29, 31, sameBar.toString()),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));
    }

    @Test
    void levelMapping_rangesAndBoundaries_singleInstance() {
        int i = 0;

        assertEquals(SignalLevel.STRONG, call(29, 31, 29.9, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.STRONG, call(29, 31,30.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.MIDDLE, call(29, 31,35.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.MIDDLE, call(29, 31,40.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.SMALL,  call(29, 31,41.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.SMALL,  call(29, 31,50.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.MIDDLE, call(71, 69,60.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.MIDDLE, call(71, 69,65.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.STRONG, call(71, 69,70.0, i++).getLevel()); resetGate(i++);
        assertEquals(SignalLevel.STRONG, call(71, 69,75.0, i++).getLevel());
    }

    @Test
    void firstCall_withoutPrevReturnsNull_andInitializesState() {
        // lastValue=null → первый вызов должен вернуть null и инициализировать состояние
        assertNull(strategy.generate(
                h1NoPrev(55, "2025-01-01T09:00:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));

        // Следом нормальный триггер
        assertNotNull(strategy.generate(
                h1(29, 31, "2025-01-01T09:01:00Z"),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        ));
    }

    Signal call(double prev, double cur, double daily, int i) {
        String h1Bucket = String.format("2025-01-01T13:%02d:00Z", i);
        return strategy.generate(
                h1(prev, cur, h1Bucket),                       // пересечение 30↑
                d1(daily, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        );
    }

    void resetGate(int i) {
        // пересекаем 50 (любой стороной) → canEmit=true на следующем вызове
        strategy.generate(
                h1(49, 51, String.format("2025-01-01T13:%02d:00Z", i)),
                d1(45.0, "2025-01-01T00:00:00Z"),
                BigDecimal.ONE,
                StrategyKind.RSI_DUAL_TF
        );
    }
}
