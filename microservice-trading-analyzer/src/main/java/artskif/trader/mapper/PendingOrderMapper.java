package artskif.trader.mapper;

import artskif.trader.entity.OrderState;
import artskif.trader.entity.PendingOrder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Mapper для преобразования данных из OKX API в сущность PendingOrder
 */
@ApplicationScoped
public class PendingOrderMapper {

    private static final Logger log = LoggerFactory.getLogger(PendingOrderMapper.class);

    /**
     * Преобразует Map из OKX API в сущность PendingOrder
     *
     * @param data данные ордера из API OKX
     * @return сущность PendingOrder
     */
    public PendingOrder mapToEntity(Map<String, Object> data) {
        try {
            PendingOrder order = new PendingOrder();

            // Обязательные поля
            order.clOrdId = getStringValue(data, "clOrdId");
            order.instId = getStringValue(data, "instId");
            order.instType = getStringValue(data, "instType");
            order.px = getBigDecimalValue(data, "px");
            order.sz = getBigDecimalValue(data, "sz");
            order.side = getStringValue(data, "side");
            order.tdMode = getStringValue(data, "tdMode");
            order.lever = getBigDecimalValue(data, "lever");

            // Дополнительные поля
            order.ordId = getStringValue(data, "ordId");
            order.state = OrderState.fromString(getStringValue(data, "state"));
            order.ordType = getStringValue(data, "ordType");

            // Временные метки
            order.createdAt = Instant.now();
            order.updatedAt = Instant.now();

            return order;
        } catch (Exception e) {
            log.error("❌ Ошибка при преобразовании Map в PendingOrder: {}", data, e);
            throw new RuntimeException("Ошибка преобразования данных ордера", e);
        }
    }

    /**
     * Получает строковое значение из Map
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Получает BigDecimal значение из Map
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }

        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(strValue);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Не удалось преобразовать значение '{}' в BigDecimal для поля '{}'", strValue, key);
                return null;
            }
        }

        log.warn("⚠️ Неожиданный тип данных для поля '{}': {}", key, value.getClass().getName());
        return null;
    }
}
