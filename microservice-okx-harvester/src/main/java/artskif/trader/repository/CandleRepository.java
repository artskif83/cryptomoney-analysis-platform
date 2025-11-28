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
     * Находит все временные разрывы (гапы) в последовательности свечей за указанный период.
     *
     * @param symbol символ инструмента
     * @param timeframe таймфрейм свечей
     * @param candleDuration длительность одной свечи
     * @param startEpochMs начальная граница поиска (самая ранняя дата)
     * @return Список всех найденных гапов, отсортированный от новых к старым (ближайший к now первый)
     */
    @Transactional
    public List<TimeGap> findAllGaps(String symbol, String timeframe, Duration candleDuration, long startEpochMs) {
        try {
            Instant now = Instant.now();
            Instant startBoundary = Instant.ofEpochMilli(startEpochMs);

            LOG.infof("🔍 Поиск всех гапов: symbol=%s tf=%s от %s до %s, duration=%s",
                    symbol, timeframe, now, startBoundary, candleDuration);

            // Получаем только timestamps свечей, отсортированные по убыванию времени
            @SuppressWarnings("unchecked")
            List<Instant> timestamps = getEntityManager()
                    .createQuery("SELECT c.id.ts FROM Candle c WHERE c.id.symbol = :symbol AND c.id.tf = :tf " +
                            "AND c.id.ts >= :start AND c.id.ts <= :end ORDER BY c.id.ts DESC")
                    .setParameter("symbol", symbol)
                    .setParameter("tf", timeframe)
                    .setParameter("start", startBoundary)
                    .setParameter("end", now)
                    .getResultList();

            if (timestamps.isEmpty()) {
                LOG.infof("⚠️ Свечи не найдены в указанном диапазоне");
                return List.of();
            }

            LOG.infof("📊 Найдено %d свечей для анализа", timestamps.size());

            List<TimeGap> gaps = new java.util.ArrayList<>();

            // Проверяем гап между now и первой свечой
            Instant firstTs = timestamps.getFirst();
            Duration gapFromNow = Duration.between(firstTs, now);
            if (gapFromNow.compareTo(candleDuration) > 0) {
                Instant gapStart = firstTs.plus(candleDuration);
                TimeGap gap = new TimeGap(gapStart, null);
                gaps.add(gap);
                LOG.infof("✅ Найден гап между first и now: %s", gap);
            }

            // Проверяем промежутки между соседними свечами
            for (int i = 0; i < timestamps.size() - 1; i++) {
                Instant currentTs = timestamps.get(i);
                Instant nextTs = timestamps.get(i + 1);

                // Вычисляем разницу между текущей и следующей свечой
                Duration gapDuration = Duration.between(nextTs, currentTs);

                // Если разница больше чем длительность одной свечи - это гап
                if (gapDuration.compareTo(candleDuration) > 0) {
                    // Гап найден: от nextTs + candleDuration до currentTs
                    Instant gapStart = nextTs.plus(candleDuration);
                    Instant gapEnd = currentTs;
                    TimeGap gap = new TimeGap(gapStart, gapEnd);
                    gaps.add(gap);
                    LOG.infof("✅ Найден гап в последовательности: %s", gap);
                }
            }

            // Проверяем гап между последней свечой и startBoundary
            Instant lastTs = timestamps.getLast();
            Duration gapToStart = Duration.between(startBoundary, lastTs);
            if (gapToStart.compareTo(candleDuration) > 0) {
                Instant gapStart = startBoundary;
                Instant gapEnd = lastTs;
                TimeGap gap = new TimeGap(gapStart, gapEnd);
                gaps.add(gap);
                LOG.infof("✅ Найден гап между last и startBoundary: %s", gap);
            }

            if (gaps.isEmpty()) {
                LOG.infof("✅ Гапов не найдено в последовательности свечей");
            } else {
                LOG.infof("✅ Всего найдено гапов: %d", gaps.size());
            }

            return gaps;

        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при поиске гапов: symbol=%s tf=%s", symbol, timeframe);
            return List.of();
        }
    }
}


