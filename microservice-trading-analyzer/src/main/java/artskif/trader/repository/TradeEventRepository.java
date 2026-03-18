package artskif.trader.repository;

import artskif.trader.entity.TradeEventEntity;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * Репозиторий для работы с торговыми событиями
 */
@ApplicationScoped
public class TradeEventRepository implements PanacheRepositoryBase<TradeEventEntity, TradeEventEntity.TradeEventId> {

    private static final Logger LOG = Logger.getLogger(TradeEventRepository.class);

    /**
     * Сохраняет торговое событие в БД
     *
     * @param entity событие для сохранения
     * @return сохраненное событие
     */
    @Transactional
    public TradeEventEntity save(TradeEventEntity entity) {
        try {
            persist(entity);
            LOG.debugf("✅ TradeEvent сохранен в БД: uuid=%s, type=%s, instrument=%s, direction=%s, timeframe=%s, tag=%s, timestamp=%s",
                    entity.uuid, entity.eventType, entity.instrument, entity.direction,
                    entity.id.timeframe, entity.id.tag, entity.id.timestamp);
            return entity;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении TradeEvent: %s", entity);
            throw e;
        }
    }

    /**
     * Находит события по инструменту за указанный период
     *
     * @param instrument название инструмента
     * @param from начало периода
     * @param to конец периода
     * @return список событий
     */
    public List<TradeEventEntity> findByInstrumentAndPeriod(String instrument, Instant from, Instant to) {
        return list("instrument = ?1 and timestamp >= ?2 and timestamp <= ?3 order by timestamp desc",
                instrument, from, to);
    }

    /**
     * Находит события по типу и направлению
     *
     * @param eventType тип события
     * @param direction направление сделки
     * @return список событий
     */
    public List<TradeEventEntity> findByTypeAndDirection(TradeEventType eventType, Direction direction) {
        return list("eventType = ?1 and direction = ?2 order by timestamp desc", eventType, direction);
    }

    /**
     * Находит последние N событий
     *
     * @param limit количество событий
     * @return список событий
     */
    public List<TradeEventEntity> findLatest(int limit) {
        return find("order by timestamp desc").page(0, limit).list();
    }

    /**
     * Находит события только для тестового или боевого режима
     *
     * @param isTest флаг тестового режима
     * @return список событий
     */
    public List<TradeEventEntity> findByTestMode(boolean isTest) {
        return list("isTest = ?1 order by timestamp desc", isTest);
    }
}
