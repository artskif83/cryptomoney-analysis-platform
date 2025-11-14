package artskif.trader.repository;

import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

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
}

