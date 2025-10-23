package artskif.trader.mapper;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CandlestickMapper {

    private static final Logger LOG = Logger.getLogger(CandlestickMapper.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Преобразование одной строки OKX в доменную свечу. */
    public static CandlestickDto mapCandlestickHistoryNodeToDto(JsonNode node) {
        CandlestickDto candle = new CandlestickDto();
        candle.setTimestamp(node.get(0).asLong());
        candle.setOpen(new BigDecimal(node.get(1).asText()));
        candle.setHigh(new BigDecimal(node.get(2).asText()));
        candle.setLow(new BigDecimal(node.get(3).asText()));
        candle.setClose(new BigDecimal(node.get(4).asText()));
        candle.setConfirmed("1".equals(node.get(5).asText()));
        return candle;
    }

    /** Возвращает пусто, если сообщение служебное или некорректное */
    public static Optional<CandlestickPayloadDto> map(String json) {
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
                    lastCandle = getCandlestickDto(entryNode);
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

    /** Преобразование одной строки OKX в доменную свечу */
    private static CandlestickDto getCandlestickDto(JsonNode node) {
        CandlestickDto candle = new CandlestickDto();
        candle.setTimestamp(node.get(0).asLong());
        candle.setOpen(new BigDecimal(node.get(1).asText()));
        candle.setHigh(new BigDecimal(node.get(2).asText()));
        candle.setLow(new BigDecimal(node.get(3).asText()));
        candle.setClose(new BigDecimal(node.get(4).asText()));
        candle.setVolume(new BigDecimal(node.get(5).asText()));
        candle.setVolumeCcy(new BigDecimal(node.get(6).asText()));
        candle.setVolumeCcyQuote(new BigDecimal(node.get(7).asText()));
        candle.setConfirmed("1".equals(node.get(8).asText()));
        return candle;
    }
}
