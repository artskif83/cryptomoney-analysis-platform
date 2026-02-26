package artskif.trader.mapper;

import artskif.trader.entity.OrderState;
import artskif.trader.entity.Position;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mapper для преобразования данных из Trading Executor (OKX) в сущность Position.
 *
 * Основан на ответе OKX API: /api/v5/account/positions
 */
@ApplicationScoped
public class PositionMapper {

    private static final Logger log = LoggerFactory.getLogger(PositionMapper.class);

    public Position mapToEntity(Map<String, Object> data) {
        try {
            Position position = new Position();

            String instId = getStringValue(data, "instId");
            String mgnMode = getStringValue(data, "mgnMode"); // cross/isolated

            // Размер позиции: pos (может быть отрицательным в net режиме)
            BigDecimal pos = getBigDecimalValue(data, "pos");
            BigDecimal absPos = pos == null ? null : pos.abs();

            // posSide: long/short/net. В net режиме знак pos определяет направление.
            String apiPosSide = getStringValue(data, "posSide");
            String resolvedPosSide = resolvePosSide(apiPosSide, pos);

            // Детерминированный primary key
            position.posId = getStringValue(data, "posId");

            position.instId = instId;
            position.instType = getStringValue(data, "instType");
            position.tdMode = mgnMode;

            // Сохраняем sz как абсолютный размер, направление хранится в side
            position.sz = absPos;

            BigDecimal markPx = getBigDecimalValue(data, "markPx");
            BigDecimal avgPx = getBigDecimalValue(data, "avgPx");
            position.px = avgPx != null ? avgPx : markPx;

            position.lever = getBigDecimalValue(data, "lever");

            // side храним как posSide (long/short/net)
            position.posSide = resolvedPosSide;

            position.slTriggerPx = extractStopLossPrice(data);

            position.cTime = getInstantFromMillis(data, "cTime");
            position.uTime = getInstantFromMillis(data, "uTime");

            // state: если abs(pos) == 0 (или null) => CLOSED, иначе LIVE
            position.state = resolveStateFromSize(absPos);

            if (position.posId == null || position.posId.isBlank()) {
                throw new IllegalArgumentException("posId не может быть null/пустым");
            }
            if (position.instId == null || position.instId.isBlank()) {
                log.debug("Position posId={} без instId: data={}", position.posId, data);
            }

            return position;
        } catch (Exception e) {
            log.error("❌ Ошибка при преобразовании Map в Position: {}", data, e);
            throw new RuntimeException("Ошибка преобразования данных позиции", e);
        }
    }

    private String resolvePosSide(String apiPosSide, BigDecimal pos) {
        if (apiPosSide == null || apiPosSide.isBlank()) {
            // Если posSide не пришёл, ведём себя как net
            return posSideFromNetPos(pos);
        }

        String normalized = apiPosSide.trim().toLowerCase();
        // В long/short режиме OKX всегда отдаёт pos положительным.
        if ("long".equals(normalized) || "short".equals(normalized)) {
            return normalized;
        }

        // net: знак pos определяет направление
        return posSideFromNetPos(pos);
    }

    private String posSideFromNetPos(BigDecimal pos) {
        if (pos == null) {
            return "net";
        }
        int cmp = pos.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            return "long";
        }
        if (cmp < 0) {
            return "short";
        }
        return "net";
    }

    private OrderState resolveStateFromSize(BigDecimal pos) {
        if (pos == null) {
            return OrderState.CLOSED;
        }
        return pos.compareTo(BigDecimal.ZERO) == 0 ? OrderState.CLOSED : OrderState.LIVE;
    }

    /**
     * Извлекает цену триггера стоп-лосса из closeOrderAlgo.
     * OKX возвращает массив SL/TP алго-ордеров в поле closeOrderAlgo позиции.
     * Берём первую запись, в которой заполнено slTriggerPx.
     *
     * @param data данные позиции из API OKX
     * @return цена триггера стоп-лосса или null, если не найдена
     */
    private BigDecimal extractStopLossPrice(Map<String, Object> data) {
        try {
            Object closeOrderAlgoObj = data.get("closeOrderAlgo");

            if (closeOrderAlgoObj == null) {
                return null;
            }

            if (closeOrderAlgoObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> closeOrderAlgo = (List<Map<String, Object>>) closeOrderAlgoObj;

                for (Map<String, Object> algoOrd : closeOrderAlgo) {
                    BigDecimal slTriggerPx = getBigDecimalValue(algoOrd, "slTriggerPx");
                    if (slTriggerPx != null) {
                        log.debug("📍 Найден SL позиции с ценой триггера: {}", slTriggerPx);
                        return slTriggerPx;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("⚠️ Ошибка при извлечении SL из closeOrderAlgo: {}", e.getMessage());
            return null;
        }
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : value.toString();
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        if (value instanceof String s) {
            if (s.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Не удалось преобразовать '{}' в BigDecimal для поля '{}'", s, key);
                return null;
            }
        }

        log.warn("⚠️ Неожиданный тип данных для поля '{}': {}", key, value.getClass().getName());
        return null;
    }

    private Instant getInstantFromMillis(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        try {
            long millis;
            if (value instanceof Long l) {
                millis = l;
            } else if (value instanceof Number n) {
                millis = n.longValue();
            } else if (value instanceof String s) {
                if (s.isEmpty()) {
                    return null;
                }
                millis = Long.parseLong(s);
            } else {
                return null;
            }
            return Instant.ofEpochMilli(millis);
        } catch (Exception e) {
            log.warn("⚠️ Не удалось преобразовать '{}' в Instant для поля '{}'", value, key);
            return null;
        }
    }
}
