package artskif.trader.candle;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import jakarta.annotation.PostConstruct;

import java.time.Instant;




public abstract class AbstractTimeSeriesTicker extends AbstractTimeSeries<CandlestickDto> implements CandleTicker {

    protected abstract CandleEventBus getEventBus();
    protected abstract CandleType getCandleType();

    @PostConstruct
    void init() {
        restoreBuffer();
    }

    public synchronized void handleTick(String message) {
        try {
            //System.out.println("📥 [" + getName() + "] Пришло сообщение: " + message);

            CandlestickPayloadDto candlestickPayloadDto = CandlestickMapper.map(message);
            CandlestickDto candle = candlestickPayloadDto.getCandle();

            Instant bucket = Instant.ofEpochMilli(candle.getTimestamp());
            getBuffer().putItem(bucket, candle);
            // Если новый тик принадлежит новой свече — подтвердить предыдущую
            if (Boolean.TRUE.equals(candle.getConfirmed())) {
                getEventBus().publish(new CandleEvent(getCandleType(), candlestickPayloadDto.getInstrumentId(), bucket, candle));
                saveBuffer();
            }
        } catch (Exception e) {
            System.out.println("❌ [" + getName() + "] Не удалось разобрать сообщение: " + message);
        }
    }

}
