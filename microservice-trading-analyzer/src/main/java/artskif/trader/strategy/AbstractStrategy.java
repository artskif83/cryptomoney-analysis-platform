package artskif.trader.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.TradeEventProcessor;
import io.quarkus.logging.Log;
import org.ta4j.core.*;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.TradeOnCurrentCloseModel;
import org.ta4j.core.criteria.NumberOfPositionsCriterion;
import org.ta4j.core.criteria.NumberOfWinningPositionsCriterion;
import org.ta4j.core.criteria.PositionsRatioCriterion;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractStrategy implements CandleEventListener {

    // Общие константы для расчетов
    protected static final DecimalNum ONE = DecimalNum.valueOf(1);
    protected static final DecimalNum HUNDRED = DecimalNum.valueOf(100);

    protected Integer lastProcessedBarIndex = null;
    protected BaseBarSeries lifetimeBarSeries;
    private final AtomicBoolean running = new AtomicBoolean(false); // флаг запуска старатегии

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

        Log.infof("📦 Запущен иснстанс стратегии: %s", this.getClass().getSimpleName());
    }

    public void startStrategy() {
        Log.infof("🚀 Запуск стратегии для лайв-торговли: %s", getName());
        checkColumnsExist(getLifetimeSchema());
        lifetimeBarSeries = candle.getInstance(getTimeframe()).getLiveBarSeries();

        setRunning(true);
    }

    public void stopStrategy() {
        Log.infof("🛑 Остановка стратегии для лайв-торговли: %s", getName());
        lifetimeBarSeries = null;
        setRunning(false);
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

        checkColumnsExist(getBacktestSchema());

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();
        int totalBars = historicalBarSeries.getBarCount();
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        List<DatabaseSnapshot> dbRows = new ArrayList<>();
        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        TradingRecord tradingRecord = getTradingRecord(historicalBarSeries);
        TradeOnCurrentCloseModel tradeExecutionModel = new TradeOnCurrentCloseModel();

        int processedCount = 0;
        for (int index = historicalBarSeries.getBeginIndex(); index <= historicalBarSeries.getEndIndex(); index++) {

            // Хук для обработки каждой свечи - здесь можно открывать/закрывать позиции и сохранять метрики
            if (tradingRecord != null && tradeEventProcessor != null) {
                additionalColumns = captureBacktestPositionMetrics(index, historicalBarSeries, tradingRecord, tradeExecutionModel);
            }

            Bar bar = historicalBarSeries.getBar(index);
            DatabaseSnapshot dbRow = snapshotBuilder.build(bar, getBacktestSchema(), additionalColumns, index, false);
            dbRows.add(dbRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (index > 0 && (index % progressStep == 0 || index == totalBars - 1)) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.infof("⏳ Прогресс тестирования: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }

        if (tradingRecord != null) {
            Log.info("📊 Выполняем торговый анализ стратегии...");
            strategyAnalysis(tradingRecord, historicalBarSeries);
        } else {
            Log.info("⚠️ Торговый анализ пропущен - TradingRecord не инициализирован");
        }

        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(dbRows);

        Log.infof("✅ Завершено тестирование. Обработано %d свечей", processedCount);
    }

    private TradingRecord getTradingRecord(BaseBarSeries historicalBarSeries) {
        TradingRecord tradingRecord = null;

        if (tradeEventProcessor != null) {
            // Инициализация торговых моделей
            ZeroCostModel transactionCostModel = new ZeroCostModel();
            ZeroCostModel holdingCostModel = new ZeroCostModel();

            tradingRecord = new BaseTradingRecord(
                    tradeEventProcessor.getTradeType(),
                    historicalBarSeries.getBeginIndex(),
                    historicalBarSeries.getEndIndex(),
                    transactionCostModel,
                    holdingCostModel
            );
        }
        return tradingRecord;
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
     * Хук для обработки каждого бара в процессе бэктеста.
     * Переопределяйте в подклассах для добавления специфичной логики (например, открытие/закрытие позиций).
     *
     * @param index               индекс текущего бара
     * @param historicalBarSeries серия исторических данных
     * @param tradingRecord       торговый рекорд для управления позициями
     * @param tradeExecutionModel модель исполнения сделок
     * @return дополнительные колонки для сохранения в БД (например, позиции, стоп-лосс, тейк-профит)
     */
    protected Map<ColumnTypeMetadata, Num> captureBacktestPositionMetrics(int index,
                                                                          BaseBarSeries historicalBarSeries,
                                                                          TradingRecord tradingRecord,
                                                                          TradeOnCurrentCloseModel tradeExecutionModel) {

        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        // Торговая логика
        boolean shouldOperate = false;
        Position position = tradingRecord.getCurrentPosition();

        if (position.isNew()) {
            shouldOperate = !isUnstableAt(index) && tradeEventProcessor.shouldEnter(index, tradingRecord, false);
        } else if (position.isOpened()) {
            shouldOperate = !isUnstableAt(index) && tradeEventProcessor.shouldExit(index, tradingRecord, false);
        }

        if (shouldOperate) {
            tradeExecutionModel.execute(index, tradingRecord, historicalBarSeries, historicalBarSeries.numFactory().one());
        }

        // Обновление дополнительных колонок
        if (position.isOpened()) {
            Num netPrice = position.getEntry().getNetPrice();
            Num stopLoss = netPrice.multipliedBy(ONE.plus(tradeEventProcessor.getStoplossPercentage().dividedBy(HUNDRED)));
            Num takeProfit = netPrice.multipliedBy(ONE.minus(tradeEventProcessor.getTakeprofitPercentage().dividedBy(HUNDRED)));

            additionalColumns.put(PositionColumn.PositionColumnType.POSITION_PRICE_1M, netPrice);
            additionalColumns.put(PositionColumn.PositionColumnType.STOPLOSS_1M, stopLoss);
            additionalColumns.put(PositionColumn.PositionColumnType.TAKEPROFIT_1M, takeProfit);
        }
        return additionalColumns;
    }

    /**
     * Получить схему данных для бэктеста
     */
    protected abstract AbstractSchema getBacktestSchema();

    /**
     * Получить схему данных для лайв-торговли
     */
    protected abstract AbstractSchema getLifetimeSchema();

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
     * Проверка и создание колонок для схемы в базе данных
     *
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
