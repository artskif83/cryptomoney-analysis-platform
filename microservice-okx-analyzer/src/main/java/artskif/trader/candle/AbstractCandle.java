package artskif.trader.candle;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickHistoryDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;


public abstract class AbstractCandle extends AbstractTimeSeries<CandlestickDto> implements CandleTicker {

    protected static final String DEFAULT_SYMBOL = "BTC-USDT";

    protected abstract CandleEventBus getEventBus();

    @Override
    protected String getSymbol() {
        return DEFAULT_SYMBOL;
    }

    @PostConstruct
    void init() {
        initRestoreBuffer();
    }

    /**
     * Восстанавливает буфер из пачки истории (JSON-массив OKX /history-*-candles).
     * message: строка массива data, например:
     * [[1698796800000,"34300","34500","34000","34210",...], [...], ...]
     */
    public synchronized void restoreFromHistory(String message) {
        try {
            CandlestickHistoryDto historyDto = CandlestickMapper.mapJsonMessageToCandlestickMap(message, getCandleTimeframe());

            if (historyDto.getData().isEmpty()) {
                log().warnf("⚠️ [%s] После парсинга история пуста", getName());
                return;
            }

            // Единым снимком, без нарушения последовательности:
            getHistoricalBuffer().restoreItems(historyDto.getData());
            log().infof("✅ [%s] Добавили в исторический буфер %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                    getName(), historyDto.getData().size(), getHistoricalBuffer().size(), historyDto.getInstId(), historyDto.isLast());
            if (historyDto.isLast()) {
                getLiveBuffer().restoreItems(historyDto.getData());
                log().infof("✅ [%s] Добавили в актуальный буфер %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                        getName(), historyDto.getData().size(), getLiveBuffer().size(), historyDto.getInstId(), historyDto.isLast());
            }
            initSaveBuffer();
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось обработать элементы для истории: %s", getName(), e.getMessage());
        }
    }

    public synchronized void handleTick(String message) {
        try {
            CandlestickPayloadDto candlestickPayloadDto;
            Optional<CandlestickPayloadDto> opt = CandlestickMapper.map(message, getCandleTimeframe());
            if (opt.isPresent()) {
                candlestickPayloadDto = opt.get();
            } else {
                return;
            }

            CandlestickDto candle = candlestickPayloadDto.getCandle();

            Instant bucket = candle.getTimestamp();
            // Если новый тик принадлежит новой свече — подтвердить предыдущую
            if (Boolean.TRUE.equals(candle.getConfirmed())) {
                getLiveBuffer().putItem(bucket, candle);
                initSaveBuffer();
                getEventBus().publish(new CandleEvent(getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle, candle.getConfirmed()));
            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось разобрать сообщение - %s. Ошибка - %s", getName(), message, e.getMessage());
        }
    }

}
