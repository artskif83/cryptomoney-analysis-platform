package artskif.trader.repository;

import artskif.trader.entity.OrderCreationParams;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Репозиторий для работы с параметрами создания ордеров.
 */
@ApplicationScoped
public class OrderCreationParamsRepository implements PanacheRepositoryBase<OrderCreationParams, Long> {

    private static final Logger LOG = Logger.getLogger(OrderCreationParamsRepository.class);

    /**
     * Сохраняет параметры создания ордера в БД.
     *
     * @param params объект параметров для сохранения
     * @return сохранённый объект
     */
    @Transactional
    public OrderCreationParams save(OrderCreationParams params) {
        try {
            persist(params);
            LOG.debugf("✅ OrderCreationParams сохранены: id=%s, trendStrength=%s, longDepositRisk=%s, shortDepositRisk=%s, closeOppositeLong=%s, closeOppositeShort=%s",
                    params.id, params.trendStrength, params.longDepositRiskPercent,
                    params.shortDepositRiskPercent, params.closeOppositeLong, params.closeOppositeShort);
            return params;
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка при сохранении OrderCreationParams: %s", params);
            throw e;
        }
    }

    /**
     * Возвращает все записи, отсортированные по дате создания (новые сначала).
     *
     * @return список параметров
     */
    public List<OrderCreationParams> findAllOrderedByCreatedAt() {
        return list("ORDER BY createdAt DESC");
    }

    /**
     * Возвращает последнюю (самую свежую) запись параметров.
     *
     * @return последние параметры или null
     */
    public OrderCreationParams findLatest() {
        return find("ORDER BY createdAt DESC").firstResult();
    }

    /**
     * Возвращает параметры создания ордера по значению trendStrength.
     *
     * @param trendStrength значение силы тренда
     * @return найденный объект параметров или null
     */
    public OrderCreationParams findByTrendStrength(int trendStrength) {
        return find("trendStrength", trendStrength).firstResult();
    }
}

