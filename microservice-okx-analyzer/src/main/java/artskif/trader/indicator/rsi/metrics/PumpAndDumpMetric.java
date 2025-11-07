package artskif.trader.indicator.rsi.metrics;

import artskif.trader.buffer.Buffer;
import artskif.trader.indicator.rsi.RsiPoint;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class PumpAndDumpMetric extends AbstractMetrics {

    private static final BigDecimal PUMP_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal DUMP_THRESHOLD = new BigDecimal("30");

    @Override
    public void recalculateMetric(Buffer<RsiPoint> rsiBuffer) {
        Map<Instant, RsiPoint> snapshot = rsiBuffer.getSnapshot();

        if (snapshot.isEmpty()) {
            return;
        }

        RsiPoint previousPoint = null;

        for (Map.Entry<Instant, RsiPoint> entry : snapshot.entrySet()) {
            RsiPoint currentPoint = entry.getValue();

            if (previousPoint != null && currentPoint.rsi() != null && previousPoint.rsi() != null) {
                boolean isPump = detectPump(previousPoint.rsi(), currentPoint.rsi());
                boolean isDump = detectDump(previousPoint.rsi(), currentPoint.rsi());

                // Создаем новую точку с обновленными метриками, если обнаружены изменения
                if (isPump || isDump) {
                    RsiPoint updatedPoint = new RsiPoint(
                            currentPoint.bucket(),
                            currentPoint.rsi(),
                            currentPoint.timeframe(),
                            isPump ? true : null,
                            isDump ? true : null
                    );
                    rsiBuffer.putItem(entry.getKey(), updatedPoint);
                }
            }

            previousPoint = currentPoint;
        }
    }

    /**
     * Обнаруживает pump: пересечение уровня 70 сверху вниз.
     * @param previousRsi предыдущее значение RSI
     * @param currentRsi текущее значение RSI
     * @return true если обнаружен pump
     */
    private boolean detectPump(BigDecimal previousRsi, BigDecimal currentRsi) {
        return previousRsi.compareTo(PUMP_THRESHOLD) > 0 &&
               currentRsi.compareTo(PUMP_THRESHOLD) <= 0;
    }

    /**
     * Обнаруживает dump: пересечение уровня 30 снизу вверх.
     * @param previousRsi предыдущее значение RSI
     * @param currentRsi текущее значение RSI
     * @return true если обнаружен dump
     */
    private boolean detectDump(BigDecimal previousRsi, BigDecimal currentRsi) {
        return previousRsi.compareTo(DUMP_THRESHOLD) < 0 &&
               currentRsi.compareTo(DUMP_THRESHOLD) >= 0;
    }
}
