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
     * Помечает позиции как CLOSED, которых нет в текущем списке (синхронизация с биржей).
     */
    @Transactional
    public long markAsClosedNotIn(List<String> posIds) {
        if (posIds.isEmpty()) {
            long updated = update("state = ?1 where state != ?2", OrderState.CLOSED, OrderState.CLOSED);
            if (updated > 0) {
                LOG.infof("🔒 Помечено позиций как CLOSED: %d", updated);
            }
            return updated;
        }

        long updated = update("state = ?1 where posId not in ?2 and state != ?3",
                OrderState.CLOSED, posIds, OrderState.CLOSED);
        if (updated > 0) {
            LOG.infof("🔒 Помечено позиций как CLOSED: %d", updated);
        }
        return updated;
    }
}
