package artskif.trader.candle;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.StreamSupport;


public abstract class AbstractTimeSeriesTicker extends AbstractTimeSeries<CandlestickDto> implements CandleTicker {

    protected abstract CandleEventBus getEventBus();
    protected abstract CandleTimeframe getCandleTimeframe();

    @PostConstruct
    void init() {
        restoreBuffer();
    }

    /**
     * Восстанавливает буфер из пачки истории (JSON-массив OKX /history-*-candles).
     * message: строка массива data, например:
     * [[1698796800000,"34300","34500","34000","34210",...], [...], ...]
     */
    public synchronized void restoreFromHistory(String message) {
        try {
            JsonNode arr = getBufferRepository().readNode(message);
            if (!arr.isArray() || arr.isEmpty()) {
                System.out.println("⚠️ [" + getName() + "] Историческая пачка пуста/не массив");
                return;
            }

            // Отсортируем по ts по возрастанию и соберём в LinkedHashMap для сохранения порядка.
            Map<Instant, CandlestickDto> ordered = new LinkedHashMap<>();

            StreamSupport.stream(arr.spliterator(), false)
                    .filter(JsonNode::isArray)
                    .map(CandlestickMapper::mapCandlestickHistoryNodeToDto)
                    .sorted(Comparator.comparingLong(CandlestickDto::getTimestamp))
                    .forEach(r -> {
                        Instant bucket = Instant.ofEpochMilli(r.getTimestamp());
                        ordered.put(bucket, r);
                    });

            if (ordered.isEmpty()) {
                System.out.println("⚠️ [" + getName() + "] После парсинга история пуста");
                return;
            }

            // Единым снимком, без нарушения последовательности:
            getBuffer().restoreItems(ordered); // Buffer.restoreItems(...) уже сделает publishSnapshot()
            saveBuffer(); // при желании можно вынести под флаг
            System.out.println("✅ [" + getName() + "] Восстановили " + ordered.size() + " элементов из истории");
        } catch (Exception e) {
            System.out.println("❌ [" + getName() + "] Не удалось восстановить историю: " + e.getMessage());
        }
    }

    public synchronized void handleTick(String message) {
        try {
            //System.out.println("📥 [" + getName() + "] Пришло сообщение: " + message);

            CandlestickPayloadDto candlestickPayloadDto = CandlestickMapper.map(message);
            CandlestickDto candle = candlestickPayloadDto.getCandle();

            Instant bucket = Instant.ofEpochMilli(candle.getTimestamp());
            getBuffer().putItem(bucket, candle);
            // Если новый тик принадлежит новой свече — подтвердить предыдущую
            getEventBus().publish(new CandleEvent(getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle));
            if (Boolean.TRUE.equals(candle.getConfirmed())) {
                saveBuffer();
            }
        } catch (Exception e) {
            System.out.println("❌ [" + getName() + "] Не удалось разобрать сообщение - %s. Ошибка - %s".formatted(message, e.getMessage()));
        }
    }

}
