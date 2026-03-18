package artskif.trader.repository;

import artskif.trader.entity.OrderState;
import artskif.trader.entity.Position;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Репозиторий для работы с открытыми позициями.
 * Уникальность позиции определяется составным ключом (posId, cTime).
 */
@ApplicationScoped
public class PositionRepository implements PanacheRepositoryBase<Position, Long> {

    private static final Logger LOG = Logger.getLogger(PositionRepository.class);

    @Transactional
    public Position save(Position position) {
        try {
            persist(position);
            LOG.infof("✅ Position сохранена: posId=%s, instId=%s, side=%s, px=%s, sz=%s",
                    position.posId, position.instId, position.posSide, position.px, position.sz);
            return position;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении Position: %s", position);
            throw e;
        }
    }

    /**
     * Найти позицию по составному ключу (posId, cTime).
     */
    public Position findByPosIdAndCTime(String posId, Instant cTime) {
        return find("posId = ?1 and cTime = ?2", posId, cTime).firstResult();
    }

    /**
     * Сохранить/обновить позиции по составному ключу (posId, cTime).
     */
    @Transactional
    public void saveAll(List<Position> positions) {
        try {
            if (positions.isEmpty()) {
                LOG.debug("Список позиций пуст, нечего сохранять");
                return;
            }

            int updated = 0;
            int inserted = 0;
            for (Position position : positions) {
                Position existing = findByPosIdAndCTime(position.posId, position.cTime);
                if (existing != null) {
                    position.id = existing.id;
                    position.createdAt = existing.createdAt;
                    position.updatedAt = Instant.now();
                    position.state = OrderState.LIVE;
                    getEntityManager().merge(position);
                    updated++;
                } else {
                    position.createdAt = Instant.now();
                    position.updatedAt = Instant.now();
                    position.state = OrderState.LIVE;
                    persist(position);
                    inserted++;
                }
            }

            LOG.debugf("✅ Позиции обработаны: обновлено=%d, вставлено=%d", updated, inserted);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении списка позиций");
            throw e;
        }
    }

    public List<Position> findByInstrument(String instId) {
        return list("instId", instId);
    }

    /**
     * @deprecated Используйте {@link #findByPosIdAndCTime(String, Instant)} для точного поиска.
     * Данный метод возвращает первую найденную запись с указанным posId.
     */
    @Deprecated
    public Position findByPosId(String posId) {
        return find("posId", posId).firstResult();
    }

    /**
     * Сохраняет исторические позиции со статусом CLOSED.
     * Если позиция с такой комбинацией (posId, cTime) уже существует и имеет статус LIVE — не перезаписывает.
     * Если позиция с такой комбинацией уже CLOSED — обновляет данные.
     * Если позиция новая — вставляет со статусом CLOSED.
     */
    @Transactional
    public void saveAllHistory(List<Position> positions) {
        try {
            if (positions.isEmpty()) {
                LOG.debug("Список исторических позиций пуст, нечего сохранять");
                return;
            }

            int updated = 0;
            int inserted = 0;
            int skipped = 0;
            for (Position position : positions) {
                Position existing = findByPosIdAndCTime(position.posId, position.cTime);
                if (existing == null) {
                    position.createdAt = Instant.now();
                    position.updatedAt = Instant.now();
                    position.state = OrderState.CLOSED;
                    persist(position);
                    inserted++;
                }
            }

            LOG.debugf("✅ Исторические позиции обработаны: вставлено=%d, обновлено=%d, пропущено(LIVE)=%d",
                    inserted, updated, skipped);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении исторических позиций");
            throw e;
        }
    }

    /**
     * Помечает позиции статусом CLOSED, если они не присутствуют в текущем снимке с биржи.
     * Затрагиваются только позиции с состоянием, отличным от CLOSED (т.е. LIVE).
     * Текущие позиции идентифицируются составным ключом (posId, cTime).
     *
     * @param positionKeys список строк вида "posId::cTimeEpochMilli" для текущих позиций
     * @return количество позиций, помеченных как CLOSED
     */
    @Transactional
    public long markAsClosedNotIn(List<String> positionKeys) {
        // Берём только не-CLOSED позиции (т.е. LIVE)
        List<Position> livePositions = list("state != ?1", OrderState.CLOSED);

        if (livePositions.isEmpty()) {
            return 0;
        }

        long marked = 0;
        for (Position pos : livePositions) {
            String key = buildPositionKey(pos.posId, pos.cTime);
            if (!positionKeys.contains(key)) {
                pos.state = OrderState.CLOSED;
                pos.updatedAt = Instant.now();
                getEntityManager().merge(pos);
                marked++;
            }
        }

        if (marked > 0) {
            LOG.debugf("🔒 Помечено CLOSED позиций, отсутствующих на бирже: %d", marked);
        }
        return marked;
    }

    /**
     * Подсчитывает количество убыточных закрытых позиций за последние 24 часа.
     * Убыточной считается позиция со статусом CLOSED и realizedPnl < 0.
     *
     * @return количество убыточных позиций за последние 24 часа
     */
    public long countLosingPositionsLast24Hours() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        return count("state = ?1 and realizedPnl < ?2 and updatedAt >= ?3",
                OrderState.CLOSED, BigDecimal.ZERO, since);
    }

    /**
     * Строит составной ключ для идентификации позиции.
     */
    public static String buildPositionKey(String posId, Instant cTime) {
        return posId + "::" + (cTime != null ? cTime.toEpochMilli() : "null");
    }
}
