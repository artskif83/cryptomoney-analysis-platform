package artskif.trader.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import artskif.trader.strategy.contract.ContractDataService;
import artskif.trader.strategy.contract.snapshot.ContractSnapshotBuilder;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.regime.MarketRegimeModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStrategy implements CandleEventListener {

    protected Integer lastProcessedBarIndex = null;
    /**
     *  Проверить, запущена ли стратегия
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Общие зависимости для всех стратегий
    protected final Candle candle;
    protected final MarketRegimeModel regimeModel;
    protected final List<EventModel> eventModels;
    protected final ContractSnapshotBuilder snapshotBuilder;
    protected final ContractDataService dataService;

    protected AbstractStrategy(Candle candle, MarketRegimeModel regimeModel, List<EventModel> eventModels,
                                ContractSnapshotBuilder snapshotBuilder, ContractDataService dataService) {
        this.candle = candle;
        this.regimeModel = regimeModel;
        this.eventModels = eventModels;
        this.snapshotBuilder = snapshotBuilder;
        this.dataService = dataService;
    }

    public abstract String getName();

    public boolean isRunning() {
        return running.get();
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
        this.running.set(isRunning);
        if (!isRunning) {
            lastProcessedBarIndex = null; // Сбрасываем при остановке
        }
    }
}
