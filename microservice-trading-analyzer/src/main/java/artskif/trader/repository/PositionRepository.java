package artskif.trader.repository;

import artskif.trader.entity.OrderState;
import artskif.trader.entity.Position;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * Репозиторий для работы с открытыми позициями.
 */
@ApplicationScoped
public class PositionRepository implements PanacheRepositoryBase<Position, String> {

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
     * Сохранить/обновить позиции по первичному ключу posId.
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
                Position existing = findById(position.posId);
                if (existing != null) {
                    position.createdAt = existing.createdAt;
                    position.updatedAt = Instant.now();
                    // если позиция снова появилась в снимке — считаем её активной
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

            LOG.infof("✅ Позиции обработаны: обновлено=%d, вставлено=%d", updated, inserted);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении списка позиций");
            throw e;
        }
    }

    public List<Position> findByInstrument(String instId) {
        return list("instId", instId);
    }

    public Position findByPosId(String posId) {
        return findById(posId);
    }

    /**
     * Сохраняет историческиe позиции со статусом CLOSED.
     * Если позиция уже существует и имеет статус LIVE — не перезаписывает её историей.
     * Если позиция уже существует со статусом CLOSED — обновляет данные.
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
                Position existing = findById(position.posId);
                if (existing != null) {
                    if (existing.state == OrderState.LIVE) {
                        // Не трогаем активные позиции историческими данными
                        skipped++;
                        continue;
                    }
                    position.createdAt = existing.createdAt;
                    position.updatedAt = Instant.now();
                    position.state = OrderState.CLOSED;
                    getEntityManager().merge(position);
                    updated++;
                } else {
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
     * Удаляет позиции в статусе LIVE, которых нет в текущем списке (синхронизация с биржей).
     */
    @Transactional
    public long deleteLiveNotIn(List<String> posIds) {
        if (posIds.isEmpty()) {
            long deleted = delete("state = ?1", OrderState.LIVE);
            if (deleted > 0) {
                LOG.debugf("🗑 Удалено LIVE-позиций (список пуст): %d", deleted);
            }
            return deleted;
        }

        long deleted = delete("posId not in ?1 and state = ?2", posIds, OrderState.LIVE);
        if (deleted > 0) {
            LOG.debugf("🗑 Удалено LIVE-позиций, отсутствующих на бирже: %d", deleted);
        }
        return deleted;
    }
}
