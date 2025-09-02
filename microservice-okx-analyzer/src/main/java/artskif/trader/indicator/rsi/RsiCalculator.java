package artskif.trader.indicator.rsi;

import artskif.trader.dto.CandlestickDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RsiCalculator {

    private static final int PERIOD = 14;
    private static final MathContext MC = MathContext.DECIMAL64;

    public static Optional<RsiPoint> computeLastRsi(Map<Instant, CandlestickDto> history, boolean includeUnconfirmed) {
        if (history == null || history.size() < PERIOD + 1) {
            return Optional.empty();
        }

        // свечи в хронологическом порядке
        List<Map.Entry<Instant, CandlestickDto>> candles = new ArrayList<>(history.entrySet());

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // инициализация через первые 14 изменений
        for (int i = 1; i <= PERIOD; i++) {
            BigDecimal prevClose = candles.get(i - 1).getValue().getClose();
            BigDecimal close = candles.get(i).getValue().getClose();

            BigDecimal change = close.subtract(prevClose, MC);
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs(MC) : BigDecimal.ZERO;

            avgGain = avgGain.add(gain, MC);
            avgLoss = avgLoss.add(loss, MC);
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(PERIOD), MC);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(PERIOD), MC);

        // скользящее сглаживание по Уайлдеру
        for (int i = PERIOD + 1; i < candles.size(); i++) {
            BigDecimal prevClose = candles.get(i - 1).getValue().getClose();
            BigDecimal close = candles.get(i).getValue().getClose();

            BigDecimal change = close.subtract(prevClose, MC);
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs(MC) : BigDecimal.ZERO;

            avgGain = (avgGain.multiply(BigDecimal.valueOf(PERIOD - 1), MC).add(gain)).divide(BigDecimal.valueOf(PERIOD), MC);
            avgLoss = (avgLoss.multiply(BigDecimal.valueOf(PERIOD - 1), MC).add(loss)).divide(BigDecimal.valueOf(PERIOD), MC);
        }

        // если нет убытков → RSI = 100
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of(new RsiPoint(candles.get(candles.size() - 1).getKey(), BigDecimal.valueOf(100)));
        }

        BigDecimal rs = avgGain.divide(avgLoss, MC);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs, MC), MC));

        Instant ts = candles.get(candles.size() - 1).getKey();
        return Optional.of(new RsiPoint(ts, rsi));
    }
}
