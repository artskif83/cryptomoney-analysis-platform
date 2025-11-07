package artskif.trader.indicator.rsi.metrics;

import artskif.trader.buffer.Buffer;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.indicator.rsi.RsiPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PumpAndDumpMetricTest {

    private PumpAndDumpMetric metric;
    private Buffer<RsiPoint> buffer;

    @BeforeEach
    void setUp() {
        metric = new PumpAndDumpMetric();
        buffer = new Buffer<>(100);
    }

    @Test
    void testDetectPump_WhenRsiCrosses70FromAbove() {
        // Arrange: создаем точки RSI, где значение пересекает 70 сверху вниз
        Instant time1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2025-01-01T10:01:00Z");

        buffer.putItem(time1, new RsiPoint(time1, new BigDecimal("75"), CandleTimeframe.CANDLE_1M));
        buffer.putItem(time2, new RsiPoint(time2, new BigDecimal("68"), CandleTimeframe.CANDLE_1M));

        // Act
        metric.recalculateMetric(buffer);

        // Assert: вторая точка должна иметь pump = true
        RsiPoint updatedPoint = buffer.getSnapshot().get(time2);
        assertNotNull(updatedPoint);
        assertEquals(Boolean.TRUE, updatedPoint.pump());
        assertNull(updatedPoint.dump());
    }

    @Test
    void testDetectDump_WhenRsiCrosses30FromBelow() {
        // Arrange: создаем точки RSI, где значение пересекает 30 снизу вверх
        Instant time1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2025-01-01T10:01:00Z");

        buffer.putItem(time1, new RsiPoint(time1, new BigDecimal("25"), CandleTimeframe.CANDLE_1M));
        buffer.putItem(time2, new RsiPoint(time2, new BigDecimal("32"), CandleTimeframe.CANDLE_1M));

        // Act
        metric.recalculateMetric(buffer);

        // Assert: вторая точка должна иметь dump = true
        RsiPoint updatedPoint = buffer.getSnapshot().get(time2);
        assertNotNull(updatedPoint);
        assertNull(updatedPoint.pump());
        assertEquals(Boolean.TRUE, updatedPoint.dump());
    }

    @Test
    void testNoPumpOrDump_WhenRsiStaysInMiddleRange() {
        // Arrange: создаем точки RSI в среднем диапазоне
        Instant time1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2025-01-01T10:01:00Z");

        buffer.putItem(time1, new RsiPoint(time1, new BigDecimal("50"), CandleTimeframe.CANDLE_1M));
        buffer.putItem(time2, new RsiPoint(time2, new BigDecimal("55"), CandleTimeframe.CANDLE_1M));

        // Act
        metric.recalculateMetric(buffer);

        // Assert: вторая точка должна остаться без изменений
        RsiPoint updatedPoint = buffer.getSnapshot().get(time2);
        assertNotNull(updatedPoint);
        assertNull(updatedPoint.pump());
        assertNull(updatedPoint.dump());
    }

    @Test
    void testEmptyBuffer() {
        // Act
        metric.recalculateMetric(buffer);

        // Assert: буфер должен остаться пустым
        assertTrue(buffer.getSnapshot().isEmpty());
    }

    @Test
    void testMultiplePoints_WithBothPumpAndDump() {
        // Arrange: создаем серию точек с pump и dump сигналами
        Instant time1 = Instant.parse("2025-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2025-01-01T10:01:00Z");
        Instant time3 = Instant.parse("2025-01-01T10:02:00Z");
        Instant time4 = Instant.parse("2025-01-01T10:03:00Z");

        buffer.putItem(time1, new RsiPoint(time1, new BigDecimal("75"), CandleTimeframe.CANDLE_1M));
        buffer.putItem(time2, new RsiPoint(time2, new BigDecimal("68"), CandleTimeframe.CANDLE_1M)); // pump
        buffer.putItem(time3, new RsiPoint(time3, new BigDecimal("25"), CandleTimeframe.CANDLE_1M));
        buffer.putItem(time4, new RsiPoint(time4, new BigDecimal("32"), CandleTimeframe.CANDLE_1M)); // dump

        // Act
        metric.recalculateMetric(buffer);

        // Assert: проверяем pump сигнал
        RsiPoint point2 = buffer.getSnapshot().get(time2);
        assertEquals(Boolean.TRUE, point2.pump());
        assertNull(point2.dump());

        // Assert: проверяем dump сигнал
        RsiPoint point4 = buffer.getSnapshot().get(time4);
        assertNull(point4.pump());
        assertEquals(Boolean.TRUE, point4.dump());
    }
}

