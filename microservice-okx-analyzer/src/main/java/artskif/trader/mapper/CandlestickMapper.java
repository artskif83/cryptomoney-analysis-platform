package artskif.trader.mapper;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

public class CandlestickMapper {

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

    public static CandlestickPayloadDto map(String json) throws Exception {
        CandlestickMessage message = mapper.readValue(json, CandlestickMessage.class);

        CandlestickDto candle = new CandlestickDto();
        for (List<String> entry : message.getData()) {
            candle = getCandlestickDto(entry);
        }

        return new CandlestickPayloadDto(
                message.getArg().getChannel(),
                message.getArg().getInstId(),
                candle
        );
    }

    private static CandlestickDto getCandlestickDto(List<String> entry) {
        CandlestickDto candle = new CandlestickDto();
        candle.setTimestamp(Long.parseLong(entry.get(0)));
        candle.setOpen(new BigDecimal(entry.get(1)));
        candle.setHigh(new BigDecimal(entry.get(2)));
        candle.setLow(new BigDecimal(entry.get(3)));
        candle.setClose(new BigDecimal(entry.get(4)));
        candle.setVolume(new BigDecimal(entry.get(5)));
        candle.setVolumeCcy(new BigDecimal(entry.get(6)));
        candle.setVolumeCcyQuote(new BigDecimal(entry.get(7)));
        candle.setConfirmed("1".equals(entry.get(8)));
        return candle;
    }
}
