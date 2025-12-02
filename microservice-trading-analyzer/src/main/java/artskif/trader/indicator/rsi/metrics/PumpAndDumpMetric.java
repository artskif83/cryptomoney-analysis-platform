package artskif.trader.indicator.rsi.metrics;

import artskif.trader.common.Stage;
import artskif.trader.indicator.rsi.RsiPipelineContext;
import artskif.trader.indicator.rsi.RsiPoint;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class PumpAndDumpMetric implements Stage<RsiPipelineContext> {

    private static final BigDecimal PUMP_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal DUMP_THRESHOLD = new BigDecimal("30");

    @Override
    public int order() {
        return 20;
    }

    @Override
    public RsiPipelineContext process(RsiPipelineContext input) {
        RsiPoint currentPoint = input.point();

        if (currentPoint == null || currentPoint.rsi() == null) {
            return input;
        }

        Map<Instant, RsiPoint> lastNRsi = input.state().getLastNRsi();

        if (lastNRsi == null || lastNRsi.isEmpty()) {
            return input;
        }

        // Получаем последний элемент из списка
        RsiPoint previousPoint = lastNRsi.values().stream()
                .reduce((first, second) -> second)
                .orElse(null);

        if (previousPoint == null || previousPoint.rsi() == null) {
            return input;
        }

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

            return new RsiPipelineContext(
                    input.state(),
                    updatedPoint,
                    input.bucket(),
                    input.candle()
            );
        }

        return input;
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
