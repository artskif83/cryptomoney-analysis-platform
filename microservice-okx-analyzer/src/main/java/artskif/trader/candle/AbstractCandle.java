package artskif.trader.candle;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;


public abstract class AbstractCandle extends AbstractTimeSeries<CandlestickDto> implements CandleTicker {

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
            Map<Instant, CandlestickDto> ordered = CandlestickMapper.mapJsonMessageToCandlestickMap(message, getCandleTimeframe());

            if (ordered.isEmpty()) {
                log().warnf("⚠️ [%s] После парсинга история пуста", getName());
                return;
            }

            // Единым снимком, без нарушения последовательности:
            getBuffer().restoreItems(ordered); // Buffer.restoreItems(...) уже сделает publishSnapshot()
            saveBuffer(); // при желании можно вынести под флаг
            log().infof("✅ [%s] Восстановили %d элементов из истории", getName(), ordered.size());
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось восстановить историю: %s", getName(), e.getMessage());
        }
    }

    public synchronized void handleTick(String message) {
        try {
            //System.out.println("📥 [" + getName() + "] Пришло сообщение: " + message);
            CandlestickPayloadDto candlestickPayloadDto;
            Optional<CandlestickPayloadDto> opt = CandlestickMapper.map(message, getCandleTimeframe());
            if (opt.isPresent()) {
                candlestickPayloadDto = opt.get();
            } else { return; }

            CandlestickDto candle = candlestickPayloadDto.getCandle();

            Instant bucket = candle.getTimestamp();
            getBuffer().putItem(bucket, candle);
            // Если новый тик принадлежит новой свече — подтвердить предыдущую
            getEventBus().publish(new CandleEvent(getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle));
            if (Boolean.TRUE.equals(candle.getConfirmed())) {
                saveBuffer();
            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось разобрать сообщение - %s. Ошибка - %s", getName(), message, e.getMessage());
        }
    }

}
