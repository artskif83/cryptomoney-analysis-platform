package artskif.trader.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.TradeEventProcessor;

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
    protected final TradeEventProcessor tradeEventProcessor;
    protected final StrategyDataService dataService;
    protected final DatabaseSnapshotBuilder snapshotBuilder;

    protected AbstractStrategy(Candle candle, TradeEventProcessor tradeEventProcessor,
                               DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService) {
        this.candle = candle;
        this.tradeEventProcessor = tradeEventProcessor;
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
     * Метод для проведения бэктеста стратегии
     */
    public abstract void backtest();

    /**
     * Получить таймфрейм на котором работает стратегия
     */
    protected abstract CandleTimeframe getTimeframe();

    /**
     * Получить количество нестабильных баров для стратегии
     */
    protected abstract Integer getUnstableBars();

    public boolean isUnstableAt(int index) {
        return index < getUnstableBars();
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
