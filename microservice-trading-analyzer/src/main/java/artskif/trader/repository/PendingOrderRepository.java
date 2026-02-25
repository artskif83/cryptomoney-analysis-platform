package artskif.trader.repository;

import artskif.trader.entity.OrderState;
import artskif.trader.entity.PendingOrder;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * Репозиторий для работы с активными (ожидающими) ордерами
 */
@ApplicationScoped
public class PendingOrderRepository implements PanacheRepositoryBase<PendingOrder, String> {

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
            LOG.infof("✅ PendingOrder сохранен: clOrdId=%s, instId=%s, side=%s, px=%s",
                    order.clOrdId, order.instId, order.side, order.px);
            return order;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении PendingOrder: %s", order);
            throw e;
        }
    }

    /**
     * Сохраняет или обновляет список ордеров
     * Использует merge для обновления существующих записей
     *
     * @param orders список ордеров
     */
    @Transactional
    public void saveAll(List<PendingOrder> orders) {
        try {
            if (orders.isEmpty()) {
                LOG.debug("Список ордеров пуст, нечего сохранять");
                return;
            }

            // Используем merge вместо persist для обновления существующих записей
            int updated = 0;
            int inserted = 0;
            for (PendingOrder order : orders) {
                PendingOrder existing = findById(order.clOrdId);
                if (existing != null) {
                    // Обновляем существующий ордер
                    existing.instId = order.instId;
                    existing.instType = order.instType;
                    existing.px = order.px;
                    existing.sz = order.sz;
                    existing.side = order.side;
                    existing.tdMode = order.tdMode;
                    existing.lever = order.lever;
                    existing.ordId = order.ordId;
                    existing.state = order.state;
                    existing.ordType = order.ordType;
                    existing.updatedAt = Instant.now();
                    updated++;
                } else {
                    // Вставляем новый ордер
                    persist(order);
                    inserted++;
                }
            }
            LOG.infof("✅ Ордеров обновлено: %d, вставлено: %d", updated, inserted);
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении списка ордеров");
            throw e;
        }
    }

    /**
     * Находит все активные ордера для указанного инструмента
     *
     * @param instId идентификатор инструмента
     * @return список ордеров
     */
    public List<PendingOrder> findByInstrument(String instId) {
        return list("instId", instId);
    }

    /**
     * Находит все активные ордера указанного типа инструмента
     *
     * @param instType тип инструмента (например, SWAP)
     * @return список ордеров
     */
    public List<PendingOrder> findByInstrumentType(String instType) {
        return list("instType", instType);
    }

    /**
     * Находит ордер по клиентскому ID
     *
     * @param clOrdId клиентский ID ордера
     * @return ордер или null
     */
    public PendingOrder findByClOrdId(String clOrdId) {
        return findById(clOrdId);
    }

    /**
     * Помечает ордера как CLOSED, которых нет в списке (используется для синхронизации)
     *
     * @param clOrdIds список клиентских ID ордеров, которые должны остаться активными
     * @return количество помеченных ордеров
     */
    @Transactional
    public long markAsClosedNotIn(List<String> clOrdIds) {
        if (clOrdIds.isEmpty()) {
            // Если список пуст, помечаем все как закрытые
            long updated = update("state = ?1 where state != ?2", OrderState.CLOSED, OrderState.CLOSED);
            if (updated > 0) {
                LOG.infof("🔒 Помечено ордеров как CLOSED: %d", updated);
            }
            return updated;
        }
        long updated = update("state = ?1 where clOrdId not in ?2 and state != ?3",
                OrderState.CLOSED, clOrdIds, OrderState.CLOSED);
        if (updated > 0) {
            LOG.infof("🔒 Помечено ордеров как CLOSED: %d", updated);
        }
        return updated;
    }

    /**
     * Получить все активные ордера (не закрытые)
     *
     * @return список активных ордеров
     */
    public List<PendingOrder> findAllActive() {
        return list("state != ?1", OrderState.CLOSED);
    }

    /**
     * Получить все закрытые ордера
     *
     * @return список закрытых ордеров
     */
    public List<PendingOrder> findAllClosed() {
        return list("state", OrderState.CLOSED);
    }
}
