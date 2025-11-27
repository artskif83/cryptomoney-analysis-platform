package artskif.trader.repository;

import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId> {

    private static final Logger LOG = Logger.getLogger(CandleRepository.class);

    /**
     * Получает последнюю свечу для конкретного символа и таймфрейма.
     * Возвращает Optional.empty(), если записей нет.
     */
    @Transactional
    public Optional<Candle> findLatestCandle(String symbol, String timeframe) {
        try {
            Candle candle = find(
                    "id.symbol = ?1 AND id.tf = ?2 ORDER BY id.ts DESC",
                    symbol, timeframe
            ).firstResult();

            if (candle != null) {
                LOG.infof("📍 Найдена последняя свеча: symbol=%s tf=%s ts=%s",
                    symbol, timeframe, candle.id.ts);
            } else {
                LOG.infof("📍 Последняя свеча не найдена для symbol=%s tf=%s", symbol, timeframe);
            }

            return Optional.ofNullable(candle);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при поиске последней свечи: symbol=%s tf=%s", symbol, timeframe);
            return Optional.empty();
        }
    }

    /**
     * Получает timestamp последней свечи или возвращает defaultValue, если свечей нет.
     */
    @Transactional
    public long getLatestCandleTimestamp(String symbol, String timeframe, long defaultValue) {
        Optional<Candle> candleOpt = findLatestCandle(symbol, timeframe);
        if (candleOpt.isPresent()) {
            return candleOpt.get().id.ts.toEpochMilli();
        }
        return defaultValue;
    }

    /**
     * Находит ближайший к текущему времени временной разрыв (гап) в последовательности свечей.
     *
     * @param symbol символ инструмента
     * @param timeframe таймфрейм свечей
     * @param candleDuration длительность одной свечи
     * @param startEpochMs начальная граница поиска (самая ранняя дата)
     * @return Optional с TimeGap если гап найден, иначе Optional.empty()
     */
    @Transactional
    public Optional<TimeGap> findNearestGap(String symbol, String timeframe, Duration candleDuration, long startEpochMs) {
        try {
            Instant now = Instant.now();
            Instant startBoundary = Instant.ofEpochMilli(startEpochMs);

            LOG.infof("🔍 Поиск гапа: symbol=%s tf=%s от %s до %s, duration=%s",
                    symbol, timeframe, now, startBoundary, candleDuration);

            // Получаем все свечи от текущего времени до startEpochMs, отсортированные по убыванию времени
            List<Candle> candles = find(
                    "id.symbol = ?1 AND id.tf = ?2 AND id.ts >= ?3 AND id.ts <= ?4 ORDER BY id.ts DESC",
                    symbol, timeframe, startBoundary, now
            ).list();

            if (candles.isEmpty()) {
                LOG.infof("⚠️ Свечи не найдены в указанном диапазоне");
                return Optional.empty();
            }

            LOG.infof("📊 Найдено %d свечей для анализа", candles.size());

            // Проверяем промежутки между соседними свечами
            for (int i = 0; i < candles.size() - 1; i++) {
                Instant currentTs = candles.get(i).id.ts;
                Instant nextTs = candles.get(i + 1).id.ts;

                // Вычисляем разницу между текущей и следующей свечой
                Duration gap = Duration.between(nextTs, currentTs);

                // Если разница больше чем длительность одной свечи - это гап
                if (gap.compareTo(candleDuration) > 0) {
                    // Гап найден: от nextTs + candleDuration до currentTs
                    Instant gapStart = nextTs.plus(candleDuration);
                    Instant gapEnd = currentTs;

                    LOG.infof("✅ Найден гап: начало=%s конец=%s длительность=%s",
                            gapStart, gapEnd, Duration.between(gapStart, gapEnd));

                    return Optional.of(new TimeGap(gapStart, gapEnd));
                }
            }

            LOG.infof("✅ Гапов не найдено в последовательности свечей");
            return Optional.empty();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при поиске гапа: symbol=%s tf=%s", symbol, timeframe);
            return Optional.empty();
        }
    }

    /**
     * Представляет временной разрыв (гап) в последовательности свечей
     */
    public static class TimeGap {
        private final Instant start;
        private final Instant end;

        public TimeGap(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() {
            return start;
        }

        public Instant getEnd() {
            return end;
        }

        public long getStartEpochMs() {
            return start.toEpochMilli();
        }

        public long getEndEpochMs() {
            return end.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("TimeGap{start=%s, end=%s, duration=%s}",
                    start, end, Duration.between(start, end));
        }
    }
}


