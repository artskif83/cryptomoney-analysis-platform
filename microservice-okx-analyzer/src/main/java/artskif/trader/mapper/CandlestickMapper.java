package artskif.trader.mapper;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.candle.CandleTimeframe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.StreamSupport;

// Добавленные импорты для мапинга DTO -> Entity
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;

public class CandlestickMapper {

    private static final Logger LOG = Logger.getLogger(CandlestickMapper.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<Instant, CandlestickDto> mapJsonMessageToCandlestickMap(String message, CandleTimeframe period) throws JsonProcessingException {
        JsonNode root = mapper.readTree(message);
        
        // Извлекаем instId из JSON
        if (!root.has("instId") || !root.get("instId").isTextual()) {
            LOG.warnf("⚠️ Отсутствует instId в сообщении: %s", message);
            return new LinkedHashMap<>();
        }
        String instrument = root.get("instId").asText();
        
        // Извлекаем массив data
        if (!root.has("data") || !root.get("data").isArray() || root.get("data").isEmpty()) {
            LOG.warnf("⚠️ Историческая пачка пуста/не массив: %s", message);
            return new LinkedHashMap<>();
        }
        JsonNode arr = root.get("data");

        // Отсортируем по ts по возрастанию и соберём в LinkedHashMap для сохранения порядка.
        Map<Instant, CandlestickDto> ordered = new LinkedHashMap<>();

        StreamSupport.stream(arr.spliterator(), false)
                .filter(JsonNode::isArray)
                .map(node -> mapCandlestickHistoryNodeToDto(node, period, instrument))
                .sorted(Comparator.comparing(CandlestickDto::getTimestamp))
                .forEach(r -> {
                    Instant bucket = r.getTimestamp();
                    ordered.put(bucket, r);
                });

        return ordered;
    }

    /** Преобразование одной строки OKX в доменную свечу. */
    public static CandlestickDto mapCandlestickHistoryNodeToDto(JsonNode node, CandleTimeframe period, String instrument) {
        CandlestickDto candle = new CandlestickDto();
        candle.setTimestamp(Instant.ofEpochMilli(node.get(0).asLong()));
        candle.setOpen(new BigDecimal(node.get(1).asText()));
        candle.setHigh(new BigDecimal(node.get(2).asText()));
        candle.setLow(new BigDecimal(node.get(3).asText()));
        candle.setClose(new BigDecimal(node.get(4).asText()));
        candle.setConfirmed("1".equals(node.get(5).asText()));
        candle.setPeriod(period);
        candle.setInstrument(instrument);
        return candle;
    }

    /** Возвращает пусто, если сообщение служебное или некорректное */
    public static Optional<CandlestickPayloadDto> map(String json, CandleTimeframe period) {
        try {
            JsonNode root = mapper.readTree(json);

            // 1) Служебные сообщения OKX
            if (root.hasNonNull("event")) {
                LOG.warnf("Пропущено служебное сообщение (event=%s).", root.get("event").asText());
                return Optional.empty();
            }
            if (!root.has("data") || !root.get("data").isArray() || root.get("data").isEmpty()) {
                LOG.warn("Пропущено сообщение без массива 'data'.");
                return Optional.empty();
            }
            if (!root.has("arg") || !root.get("arg").isObject()) {
                LOG.warn("Пропущено сообщение без объекта 'arg'.");
                return Optional.empty();
            }

            // 2) Мапим строго типизированно
            CandlestickMessage message = mapper.treeToValue(root, CandlestickMessage.class);
            ArrayNode data = (ArrayNode) root.get("data");

            CandlestickDto lastCandle = null;
            for (JsonNode entryNode : data) {
                if (!entryNode.isArray() || entryNode.size() < 9) {
                    LOG.warnf("Пропущена некорректная свеча: %s", entryNode.toString());
                    continue;
                }
                try {
                    lastCandle = getCandlestickDto(entryNode, period, message.getArg().getInstId());
                } catch (Exception ex) {
                    LOG.warnf(ex, "Ошибка при разборе свечи: %s", entryNode.toString());
                }
            }

            if (lastCandle == null) {
                LOG.warn("Все свечи в сообщении оказались некорректными — сообщение пропущено.");
                return Optional.empty();
            }

            return Optional.of(new CandlestickPayloadDto(
                    message.getArg().getChannel(),
                    message.getArg().getInstId(),
                    lastCandle
            ));
        } catch (Exception e) {
            LOG.warnf(e, "Пропущено невалидное сообщение.");
            return Optional.empty();
        }
    }

    /** Преобразование одной строки OKX в доменную свечу */
    private static CandlestickDto getCandlestickDto(JsonNode node, CandleTimeframe period, String instrument) {
        CandlestickDto candle = new CandlestickDto();
        candle.setTimestamp(Instant.ofEpochMilli(node.get(0).asLong()));
        candle.setOpen(new BigDecimal(node.get(1).asText()));
        candle.setHigh(new BigDecimal(node.get(2).asText()));
        candle.setLow(new BigDecimal(node.get(3).asText()));
        candle.setClose(new BigDecimal(node.get(4).asText()));
        candle.setVolume(new BigDecimal(node.get(5).asText()));
        candle.setVolumeCcy(new BigDecimal(node.get(6).asText()));
        candle.setVolumeCcyQuote(new BigDecimal(node.get(7).asText()));
        candle.setConfirmed("1".equals(node.get(8).asText()));
        candle.setPeriod(period);
        candle.setInstrument(instrument);
        return candle;
    }

    // Новый метод: маппинг CandlestickDto -> сущность Candle
    public static Candle mapDtoToEntity(CandlestickDto dto) {
        if (dto == null) return null;
        Instant ts = dto.getTimestamp();
        CandleId id = new CandleId(dto.getInstrument(), dto.getPeriod().name(), ts);
        return new Candle(
                id,
                dto.getOpen(),
                dto.getHigh(),
                dto.getLow(),
                dto.getClose(),
                dto.getVolume() != null ? dto.getVolume() : BigDecimal.ZERO,
                Boolean.TRUE.equals(dto.getConfirmed())
        );
    }
}
