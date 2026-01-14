package artskif.trader.strategy;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventListener;
import artskif.trader.events.CandleEventType;
import io.quarkus.logging.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStrategy implements CandleEventListener {

    protected Integer lastProcessedBarIndex = null;
    /**
     *  Проверить, запущена ли стратегия
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public abstract String getName();

    public boolean isRunning() {
        boolean status = running.get();
        Log.debugf("🔍 Проверка статуса стратегии '%s': %b (instance: %s)",
                   getName(), status, System.identityHashCode(this));
        return status;
    }

    @Override
    public void onCandle(CandleEvent event) {
        if (event.type() != CandleEventType.CANDLE_TICK) {
            return;
        }

        if (!running.get()) {
            return;
        }

        CandlestickDto candle = event.candle();
        if (candle == null) {
            return;
        }

        onBar(candle);
    }

    /**
     * Метод вызывается при поступлении нового бара
     */
    public abstract void onBar(CandlestickDto candle);

    /**
     * Получить таймфрейм на котором работает стратегия
     */
    protected abstract CandleTimeframe getTimeframe();

    public abstract void generateHistoricalFeatures();

    /**
     * Обработать событие свечи
     * @param event событие свечи
     */
    public void handleCandleEvent(CandleEvent event) {

    }

    /**
     * Установить статус запуска стратегии
     */
    public void setRunning(boolean isRunning) {
        Log.debugf("🔧 Установка статуса стратегии '%s': %b -> %b (instance: %s)",
                   getName(), this.running.get(), isRunning, System.identityHashCode(this));
        this.running.set(isRunning);
        if (!isRunning) {
            lastProcessedBarIndex = null; // Сбрасываем при остановке
        }
    }
}
