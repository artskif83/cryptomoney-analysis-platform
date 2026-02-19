package artskif.trader.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.TradeEventProcessor;
import io.quarkus.logging.Log;
import org.ta4j.core.*;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.TradeExecutionModel;
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
import java.util.Optional;
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
    protected final TradeEventBus tradeEventBus;

    protected AbstractStrategy(Candle candle, TradeEventProcessor tradeEventProcessor,
                               DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService) {
        this(candle, tradeEventProcessor, snapshotBuilder, dataService, null);
    }

    protected AbstractStrategy(Candle candle, TradeEventProcessor tradeEventProcessor,
                               DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService,
                               TradeEventBus tradeEventBus) {
        this.candle = candle;
        this.tradeEventProcessor = tradeEventProcessor;
        this.snapshotBuilder = snapshotBuilder;
        this.dataService = dataService;
        this.tradeEventBus = tradeEventBus;

        Log.infof("📦 Запущен инстанс стратегии: %s", this.getClass().getSimpleName());
    }

    public void startStrategy() {
        Log.infof("🚀 Запуск стратегии для лайв-торговли: %s", getName());
        dataService.checkColumnsExist(getLifetimeSchema());
        lifetimeBarSeries = candle.getInstance(getTimeframe()).getLiveBarSeries();
        setRunning(true);
    }

    public void stopStrategy() {
        Log.infof("?? Остановка стратегии для лайв-торговли: %s", getName());
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

        if (event.period() != getTimeframe()) {
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
    public void onBar(CandlestickDto candle) {
        if (lifetimeBarSeries == null) {
            Log.warn("⏳ Серия баров еще не инициализирована, пропускаем обработку");
            return;
        }

        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();
        int endIndex = lifetimeBarSeries.getEndIndex();
        Bar bar = lifetimeBarSeries.getBar(endIndex);

        if (candle.getTimestamp() != bar.getBeginTime()) {
            Log.warnf(
                    "⏳ Полученный бар с timestamp %s не совпадает с последним баром серии с timestamp %s, пропускаем обработку",
                    candle.getTimestamp(),
                    bar.getBeginTime()
            );
            return;
        }

        DatabaseSnapshot dbRow = snapshotBuilder.build(bar, getName() + "-lifetime", getLifetimeSchema(), additionalColumns, endIndex, true);
        // Сохраняем в БД
        dataService.insertFeatureRow(dbRow);

        // Обработка торговых событий (если процессор настроен)
        if (tradeEventProcessor != null && tradeEventBus != null) {
            Optional<TradeEventData> tradeEvent = tradeEventProcessor.checkLifeTradeEvent(endIndex);

            if (tradeEvent.isPresent()) {
                TradeEventData eventData = tradeEvent.get();
                Log.infof(
                        "✅ Произошло торговое событие: %s %s [Процессор: %s]",
                        eventData.type(),
                        eventData.direction(),
                        tradeEventProcessor.getClass().getSimpleName()
                );

                // Публикуем событие TradeEvent
                tradeEventBus.publish(new TradeEvent(
                        eventData,
                        candle.getInstrument(),
                        candle.getTimestamp(),
                        false
                ));
            }
        }
    }

    /**
     * Метод для проведения бэктеста стратегии (Template Method)
     * Общая логика бэктеста с возможностью кастомизации через хуки
     */
    public final void backtest() {
        Log.info("📋 Начало генерации бектеста для контракта");

        dataService.checkColumnsExist(getBacktestSchema());

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();

        TradingRecord tradingRecord = processCandleSeries(historicalBarSeries, getName() + "-backtest", getBacktestSchema(), false);

        if (tradingRecord != null) {
            Log.info("📊 Выполняем торговый анализ стратегии...");
            strategyAnalysis(tradingRecord, historicalBarSeries);
        }

        Log.infof("✅ Завершено тестирование.");
    }

    private TradingRecord processCandleSeries(BarSeries barSeries, String tagName, AbstractSchema schema, boolean isLife) {
        int totalBars = barSeries.getBarCount();
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        List<DatabaseSnapshot> dbRows = new ArrayList<>();
        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        TradingRecord tradingRecord = null;
        TradeOnCurrentCloseModel tradeExecutionModel = null;

        if (!isLife) {
            tradingRecord = getTradingRecord(barSeries);
            tradeExecutionModel = new TradeOnCurrentCloseModel();
        }

        int processedCount = 0;
        for (int index = barSeries.getBeginIndex(); index <= barSeries.getEndIndex(); index++) {

            // Хук для обработки каждой свечи - здесь можно открывать/закрывать позиции и сохранять метрики
            if (tradingRecord != null && tradeEventProcessor != null) {
                additionalColumns = captureBacktestPositionMetrics(index, barSeries, tradingRecord, tradeExecutionModel);
            }

            Bar bar = barSeries.getBar(index);
            DatabaseSnapshot dbRow = snapshotBuilder.build(bar, tagName, schema, additionalColumns, index, isLife);
            dbRows.add(dbRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (index > 0 && (index % progressStep == 0 || index == totalBars - 1)) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.infof("⏳ Прогресс выполнения: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }
        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(dbRows);

        return tradingRecord;
    }

    private TradingRecord getTradingRecord(BarSeries historicalBarSeries) {
        TradingRecord tradingRecord = null;

        if (tradeEventProcessor != null) {
            // Инициализация торговых моделей
            ZeroCostModel transactionCostModel = new ZeroCostModel();
            ZeroCostModel holdingCostModel = new ZeroCostModel();

            tradingRecord = new BaseTradingRecord(
                    tradeEventProcessor.getTradeDirection() == Direction.LONG ? Trade.TradeType.BUY : Trade.TradeType.SELL,
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
        Log.debugf("Количество выигрышных позиций: %s", numberOfWiningPositions.intValue());
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
                                                                          BarSeries historicalBarSeries,
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
            Num stopLoss = netPrice.multipliedBy(ONE.plus(tradeEventProcessor.getStopLossPercentage().dividedBy(HUNDRED)));
            Num takeProfit = netPrice.multipliedBy(ONE.minus(tradeEventProcessor.getTakeProfitPercentage().dividedBy(HUNDRED)));

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
}
