package artskif.trader.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.TradeEventProcessor;
import io.quarkus.logging.Log;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.NumberOfPositionsCriterion;
import org.ta4j.core.criteria.NumberOfWinningPositionsCriterion;
import org.ta4j.core.criteria.PositionsRatioCriterion;
import org.ta4j.core.num.Num;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Метод для проведения бэктеста стратегии (Template Method)
     * Общая логика бэктеста с возможностью кастомизации через хуки
     */
    public final void backtest() {
        Log.info("📋 Начало генерации бектеста для контракта");

        AbstractSchema schema = getSchema();
        checkColumnsExist(schema);

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();
        int totalBars = historicalBarSeries.getBarCount();
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        List<DatabaseSnapshot> dbRows = new ArrayList<>();

        // Инициализация перед началом бэктеста (хук для переопределения)
        BacktestContext context = initializeBacktest(historicalBarSeries);

        int processedCount = 0;
        for (int index = historicalBarSeries.getBeginIndex(); index <= historicalBarSeries.getEndIndex(); index++) {

            // Обработка одного бара (хук для переопределения)
            processBar(index, historicalBarSeries, context);

            Bar bar = historicalBarSeries.getBar(index);
            DatabaseSnapshot dbRow = snapshotBuilder.build(bar, schema, context.additionalColumns, index, false);
            dbRows.add(dbRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (index > 0 && (index % progressStep == 0 || index == totalBars - 1)) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.infof("⏳ Прогресс тестирования: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }

        TradingRecord tradingRecord = (TradingRecord) context.customData.get("tradingRecord");

        strategyAnalysis(tradingRecord, historicalBarSeries);

        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(dbRows);

        Log.infof("✅ Завершено тестирование. Обработано %d свечей", processedCount);
    }

    private void strategyAnalysis(TradingRecord tradingRecord, BaseBarSeries historicalBarSeries) {
        Num numberOfPositions = new NumberOfPositionsCriterion().calculate(historicalBarSeries, tradingRecord);
        Log.debugf("Количество позиций: %s", numberOfPositions.intValue());
        Num numberOfWiningPositions = new NumberOfWinningPositionsCriterion().calculate(historicalBarSeries, tradingRecord);
        Log.debugf("Количество позиций: %s", numberOfPositions.intValue());
        var positionsRatio = new PositionsRatioCriterion(AnalysisCriterion.PositionFilter.PROFIT).calculate(historicalBarSeries, tradingRecord);
        Log.debugf("Соотношение выигрышных позиций: %s", positionsRatio.bigDecimalValue());

    }

    /**
     * Хук для инициализации контекста бэктеста.
     * Переопределяйте в подклассах для добавления специфичной логики (например, TradingRecord).
     *
     * @param historicalBarSeries серия исторических данных
     * @return контекст для использования в процессе бэктеста
     */
    protected BacktestContext initializeBacktest(BaseBarSeries historicalBarSeries) {
        return new BacktestContext();
    }

    /**
     * Хук для обработки одного бара в процессе бэктеста.
     * Переопределяйте в подклассах для добавления торговой логики.
     *
     * @param index индекс текущего бара
     * @param historicalBarSeries серия исторических данных
     * @param context контекст бэктеста
     */
    protected void processBar(int index, BaseBarSeries historicalBarSeries, BacktestContext context) {
        // По умолчанию ничего не делаем - простая стратегия без торговой логики
    }

    /**
     * Получить схему данных для стратегии
     */
    protected abstract AbstractSchema getSchema();

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

    /**
     * Проверка и создание колонок для схемы в базе данных
     * @param schema схема, для которой необходимо проверить колонки
     */
    protected void checkColumnsExist(AbstractSchema schema) {
        for (ContractMetadata metadata : schema.getContract().metadata) {
            dataService.ensureColumnExist(metadata.name);
        }
    }

    /**
     * Класс-контекст для хранения состояния в процессе бэктеста
     */
    public static class BacktestContext {
        /**
         * Дополнительные колонки для сохранения в БД (например, позиции, стоп-лосс, тейк-профит)
         */
        public Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        /**
         * Любые дополнительные данные, которые могут понадобиться конкретной стратегии
         */
        public Map<String, Object> customData = new HashMap<>();
    }
}
