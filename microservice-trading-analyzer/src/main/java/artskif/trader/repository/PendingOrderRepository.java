package artskif.trader.repository;

import artskif.trader.entity.PendingOrder;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * Репозиторий для работы с активными (ожидающими) ордерами.
 * Уникальность записи определяется комбинацией (ts, tf) — один снимок на временную метку.
 */
@ApplicationScoped
public class PendingOrderRepository implements PanacheRepositoryBase<PendingOrder, Long> {

    private static final Logger LOG = Logger.getLogger(PendingOrderRepository.class);

    /**
     * Сохраняет или обновляет ордер в БД
     *
     * @param order ордер для сохранения
     * @return сохраненный ордер
     */
    @Transactional
    public PendingOrder save(PendingOrder order) {
        try {
            persist(order);
            LOG.debugf("✅ PendingOrder сохранен: ordId=%s, instId=%s, side=%s, px=%s",
                    order.ordId, order.instId, order.posSide, order.px);
            return order;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении PendingOrder: %s", order);
            throw e;
        }
    }

    /**
     * Найти запись ордеров по уникальному ключу (ts, tf).
     */
    public PendingOrder findByTsAndTf(Instant ts, String tf) {
        return find("ts = ?1 and tf = ?2", ts, tf).firstResult();
    }

    /**
     * Сохранить/обновить снимок ожидающих ордеров по уникальному ключу (ts, tf).
     * Если запись с такими ts и tf уже существует — обновляет все поля.
     * Если нет — вставляет новую запись со статусом LIVE.
     */
    @Transactional
    public void saveAllByTsTf(List<PendingOrder> orders) {
        try {
            if (orders.isEmpty()) {
                LOG.debug("Список ордеров пуст, нечего сохранять");
                return;
            }

            int updated = 0;
            int inserted = 0;
            for (PendingOrder order : orders) {
                PendingOrder existing = findByTsAndTf(order.ts, order.tf);
                if (existing != null) {
                    order.id = existing.id;
                    order.createdAt = existing.createdAt;
                    order.updatedAt = Instant.now();
                    getEntityManager().merge(order);
                    updated++;
                } else {
                    order.createdAt = Instant.now();
                    order.updatedAt = Instant.now();
                    persist(order);
                    inserted++;
                }
            }

            LOG.debugf("✅ Ордеры обработаны: обновлено=%d, вставлено=%d", updated, inserted);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении списка ордеров");
            throw e;
        }
    }

    /**
     * Находит все ордера для указанного инструмента
     */
    public List<PendingOrder> findByInstrument(String instId) {
        return list("instId", instId);
    }

    /**
     * Находит все ордера указанного типа инструмента
     */
    public List<PendingOrder> findByInstrumentType(String instType) {
        return list("instType", instType);
    }
}
