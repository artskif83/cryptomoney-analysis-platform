package artskif.trader.candle;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickHistoryDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventType;
import artskif.trader.mapper.CandlestickMapper;

import java.time.Instant;
import java.util.Optional;


public abstract class AbstractCandle extends AbstractTimeSeries<CandlestickDto> implements CandleTicker {

    protected static final String DEFAULT_SYMBOL = "BTC-USDT";

    protected abstract CandleEventBus getEventBus();

    @Override
    protected String getSymbol() {
        return DEFAULT_SYMBOL;
    }

    /**
     * Восстанавливает буфер из пачки истории (JSON-массив /history-*-candles).
     * message: строка массива data, например:
     * [[1698796800000,"34300","34500","34000","34210",...], [...], ...]
     */
    public void restoreFromHistory(String message) {
        try {
            CandlestickHistoryDto historyDto = CandlestickMapper.mapJsonMessageToCandlestickMap(message, getCandleTimeframe());

            if (historyDto.getData().isEmpty()) {
                log().warnf("⚠️ [%s] После парсинга история пуста", getName());
                return;
            }

            if (historyDto.isLast()) {
                getLiveBuffer().putItems(historyDto.getData());
                getLiveBuffer().incrementVersion();
                log().infof("✅ [%s] В актуальный буфер пришло %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                        getName(), historyDto.getData().size(), getLiveBuffer().size(), historyDto.getInstId(), historyDto.isLast());

                // Обновляем live bar series если это CandleInstance
                if (this instanceof CandleInstance) {
                    for (CandlestickDto candleDto : historyDto.getData().values()) {
                        ((CandleInstance) this).addBarToLiveSeries(candleDto);
                    }
                }

                getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_TICK, getCandleTimeframe(), historyDto.getInstId(), null, null, null));
            }
            getHistoricalBuffer().putItems(historyDto.getData());
            getHistoricalBuffer().incrementVersion();
            log().infof("✅ [%s] В исторический буфер пришло %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                    getName(), historyDto.getData().size(), getHistoricalBuffer().size(), historyDto.getInstId(), historyDto.isLast());

            // Обновляем historical bar series если это CandleInstance
            if (this instanceof CandleInstance) {
                for (CandlestickDto candleDto : historyDto.getData().values()) {
                    ((CandleInstance) this).addBarToHistoricalSeries(candleDto);
                }
            }

            getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_HISTORY, getCandleTimeframe(), historyDto.getInstId(), null, null, null));

            if (historyDto.isLast()) {
                initSaveLiveBuffer();
            } else {
                initSaveHistoricalBuffer();
            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось обработать элементы для истории: %s", getName(), e.getMessage());
        }
    }

    public void handleTick(String message) {
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
                log().debugf("🕯️ [%s] Получена подтвержденная свеча: bucket=%s, o=%s, h=%s, l=%s, c=%s, v=%s",
                        getName(), bucket, candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());
                getLiveBuffer().putItem(bucket, candle);
                getHistoricalBuffer().putItem(bucket, candle);
                initSaveLiveBuffer();
                getLiveBuffer().incrementVersion();

                // Обновляем bar series если это CandleInstance
                if (this instanceof CandleInstance) {
                    ((CandleInstance) this).addBarToLiveSeries(candle);
                    ((CandleInstance) this).addBarToHistoricalSeries(candle);
                }

                getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_TICK, getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle, candle.getConfirmed()));

            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось разобрать сообщение - %s. Ошибка - %s", getName(), message, e.getMessage());
        }
    }

}
