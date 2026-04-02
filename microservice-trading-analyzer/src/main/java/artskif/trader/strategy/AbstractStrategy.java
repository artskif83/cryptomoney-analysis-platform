package artskif.trader.strategy;

import artskif.trader.broker.BrokerConfig;
import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.events.candle.CandleEventListener;
import artskif.trader.candle.CandleEventType;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractStrategy implements CandleEventListener {

    // Общие константы для расчетов
    protected static final DecimalNum ONE = DecimalNum.valueOf(1);
    protected static final DecimalNum HUNDRED = DecimalNum.valueOf(100);

    protected Integer lastProcessedBarIndex = null;
    protected BaseBarSeries lifetimeBarSeries;

    /**
     * Если true — при старте стратегия прогоняет всю накопленную серию баров через processCandleSeries.
     * Если false — стратегия сразу переходит в режим ожидания новых баров без переобработки истории.
     */
    protected boolean reprocessCandleSeries = true;

    // Промежуточная шина событий (отдельный поток)
    private final BlockingQueue<CandleEvent> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService threadProcessor;
    private volatile boolean processorRunning = true;

    // Общие зависимости для всех стратегий
    protected final Candle candle;
    protected final TradeEventProcessor shortTradeEventProcessor;
    protected final TradeEventProcessor longTradeEventProcessor;
    protected final StrategyDataService dataService;
    protected final DatabaseSnapshotBuilder snapshotBuilder;
    protected final TradeEventBus tradeEventBus;
    protected final CandleEventBus candleEventBus;
    protected final BrokerConfig brokerConfig;

    protected AbstractStrategy(Candle candle, TradeEventProcessor shortTradeEventProcessor, TradeEventProcessor longTradeEventProcessor,
                               DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService,
                               TradeEventBus tradeEventBus, CandleEventBus candleEventBus, BrokerConfig brokerConfig) {
        this.candle = candle;
        this.shortTradeEventProcessor = shortTradeEventProcessor;
        this.longTradeEventProcessor = longTradeEventProcessor;
        this.snapshotBuilder = snapshotBuilder;
        this.dataService = dataService;
        this.tradeEventBus = tradeEventBus;
        this.candleEventBus = candleEventBus;
        this.brokerConfig = brokerConfig;
        this.threadProcessor = (candle != null) ? Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Strategy-EventProcessor-" + getClass().getSimpleName());
            t.setDaemon(false);
            return t;
        }) : null;
    }

    void onStart(@Observes StartupEvent event) {
        if (candleEventBus == null || threadProcessor == null) {
            Log.warnf("⚠️ Стратегия %s не может быть запущена: candleEventBus или threadProcessor не инициализированы", getName());
            return;
        }

        if (brokerConfig != null && !brokerConfig.isAllStrategiesEnabled()) {
            Log.infof("⏸️ Все стратегии глобально отключены (strategy.all-enabled=false), стратегия %s не запущена", getName());
            return;
        }

        if (!isEnabled()) {
            Log.infof("⏸️ Стратегия %s отключена (isEnabled=false), пропускаем запуск", getName());
            return;
        }

        Log.infof("🔧 Стратегия %s запускается...", getName());

        dataService.checkColumnsExist(getLifetimeSchema());
        lifetimeBarSeries = candle.getInstance(getTimeframe()).getLiveBarSeries();
        // Запускаем поток обработки событий
        threadProcessor.submit(this::processEvents);
        candleEventBus.subscribe(this);
        processorRunning = true;

        Log.infof("✅ Стратегия запущена: %s", getName());
    }

    void onShutdown(@Observes ShutdownEvent event) {
        if (candleEventBus == null || threadProcessor == null) {
            return;
        }

        if (brokerConfig != null && !brokerConfig.isAllStrategiesEnabled()) {
            return;
        }

        if (!isEnabled()) {
            return;
        }

        Log.infof("🛑 Стратегия %s останавливается...", getName());

        // Отписываемся от событий
        candleEventBus.unsubscribe(this);

        lifetimeBarSeries = null;
        // Останавливаем поток обработки
        processorRunning = false;
        threadProcessor.shutdown();
        try {
            if (!threadProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                threadProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Log.infof("🛑 Стратегия %s остановлена", getName());
    }

    /**
     * Основной цикл обработки событий в отдельном потоке
     */
    private void processEvents() {
        Log.debugf("⚡ Поток обработки событий стратегии %s запущен", getName());

        while (processorRunning) {
            try {
                if (reprocessCandleSeries && lifetimeBarSeries.getBarCount() == lifetimeBarSeries.getMaximumBarCount()) {
                    Log.infof("🔧 Начало создания лайф графика для стратегии %s", getName());
                    processCandleSeries(lifetimeBarSeries, getName() + "-lifetime", getLifetimeSchema(), true);
                    reprocessCandleSeries = false; // Сбрасываем флаг после первой обработки серии
                    Log.infof("✅ Стратегия %s завершила создание лайф графика", getName());
                }

                CandleEvent event = eventQueue.poll(1, TimeUnit.SECONDS);

                if (event == null) {
                    continue;
                }

                handleCandleEvent(event);

            } catch (InterruptedException e) {
                Log.infof("🛑 Поток обработки событий стратегии %s прерван", getName());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при обработке события в стратегии %s", getName());
            }
        }

        Log.infof("🛑 Поток обработки событий стратегии %s остановлен", getName());
    }

    /**
     * Внутренний обработчик события свечи — выполняется в отдельном потоке
     */
    private void handleCandleEvent(CandleEvent event) {
        if (event.period() != getTimeframe()) {
            return;
        }

        if (event.type() == CandleEventType.CANDLE_TICK) {
            CandlestickDto candleDto = event.candle();
            if (candleDto == null) {
                return;
            }

            onBar(candleDto);
        } else if (event.type() == CandleEventType.CANDLE_HISTORY) {
            reprocessCandleSeries = true; // Устанавливаем флаг для переобработки серии при следующем цикле
        }
    }

    public abstract String getName();

    /**
     * Определяет, включена ли стратегия.
     * Если возвращает false — стратегия не запускается при старте приложения.
     */
    public abstract boolean isEnabled();

    @Override
    public void onCandle(CandleEvent event) {
        // Асинхронно добавляем событие в очередь, не блокируя вызывающий поток
        if (!eventQueue.offer(event)) {
            Log.warnf("⚠️ Очередь событий стратегии %s переполнена, отбрасываем CandleEvent: %s", getName(), event);
        }
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

        Log.debugf("🕯️ [%s] Обработка свечи: timestamp=%s, close=%s", getName(), candle.getTimestamp(), candle.getClose());
        DatabaseSnapshot dbRow = snapshotBuilder.build(bar, getName() + "-lifetime", getLifetimeSchema(), additionalColumns, endIndex, true);
        // Сохраняем в БД
        dataService.insertFeatureRow(dbRow);

        // Обработка торговых событий (если процессор настроен)
        TradeEventData eventData = null;

        if (shortTradeEventProcessor.shouldLimitEnter(endIndex, null, true)) {
            eventData = shortTradeEventProcessor.getLifeTradeEventData(endIndex);
        }

        if (longTradeEventProcessor.shouldLimitEnter(endIndex, null, true)) {
            eventData = longTradeEventProcessor.getLifeTradeEventData(endIndex);
        }

        if (eventData != null) {
            Log.infof(
                    "✅ Произошло торговое событие: %s %s [Процессор: %s]",
                    eventData.type(),
                    eventData.direction(),
                    shortTradeEventProcessor.getClass().getSimpleName()
            );

            // Публикуем событие TradeEvent
            tradeEventBus.publish(new TradeEvent(
                    eventData,
                    candle.getInstrument(),
                    getName() + "-lifetime",
                    candle.getTimestamp(),
                    false
            ));
        }

        Log.infof("✅ [%s] Свеча обработана стратегией: timestamp=%s, close=%s",
                getName(), candle.getTimestamp(), candle.getClose());
    }

    /**
     * Метод для проведения бэктеста стратегии (Template Method)
     * Общая логика бэктеста с возможностью кастомизации через хуки
     */
    public final void backtest() {
        backtest(null, null);
    }

    public final void backtest(Integer startIndex) {
        backtest(startIndex, null);
    }

    public final void backtest(Integer startIndex, Integer endIndex) {
        Log.info("📋 Начало генерации бектеста для контракта");

        dataService.checkColumnsExist(getBacktestSchema());

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();

        TradingRecord tradingRecord;
        if (startIndex != null || endIndex != null) {
            Log.infof("📋 Бэктест запущен с индекса: %d по индекс: %s", startIndex, endIndex);
            tradingRecord = processCandleSeries(historicalBarSeries, getName() + "-backtest", getBacktestSchema(), false, startIndex, endIndex);
        } else {
            tradingRecord = processCandleSeries(historicalBarSeries, getName() + "-backtest", getBacktestSchema(), false);
        }

        if (tradingRecord != null) {
            Log.info("📊 Выполняем торговый анализ стратегии...");
            strategyAnalysis(tradingRecord, historicalBarSeries);
        }

        Log.infof("✅ Завершено тестирование.");
    }

    private TradingRecord processCandleSeries(BarSeries barSeries, String tagName, AbstractSchema schema, boolean isLife) {
        return processCandleSeries(barSeries, tagName, schema, isLife, barSeries != null ? barSeries.getBeginIndex() : 0, null);
    }

    private TradingRecord processCandleSeries(BarSeries barSeries, String tagName, AbstractSchema schema, boolean isLife, Integer startIndex) {
        return processCandleSeries(barSeries, tagName, schema, isLife, startIndex, null);
    }

    private TradingRecord processCandleSeries(BarSeries barSeries, String tagName, AbstractSchema schema, boolean isLife, Integer startIndex, Integer endIndex) {
        if (barSeries == null || barSeries.isEmpty()) {
            Log.warnf("⚠️ BarSeries пуста или null для стратегии %s, пропускаем обработку", getName());
            return null;
        }

        int effectiveStartIndex = Math.max(startIndex != null ? startIndex : barSeries.getBeginIndex(), barSeries.getBeginIndex());
        int effectiveEndIndex = (endIndex != null) ? Math.min(endIndex, barSeries.getEndIndex()) : barSeries.getEndIndex();
        int totalBars = effectiveEndIndex - effectiveStartIndex + 1;
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        List<DatabaseSnapshot> dbRows = new ArrayList<>();
        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        TradingRecord shortTradingRecord = null;
        TradingRecord longTradingRecord = null;
        TradeOnCurrentCloseModel tradeExecutionModel = null;

        if (!isLife) {
            shortTradingRecord = getTradingRecord(barSeries, shortTradeEventProcessor.getTradeDirection());
            longTradingRecord = getTradingRecord(barSeries, longTradeEventProcessor.getTradeDirection());
            tradeExecutionModel = new TradeOnCurrentCloseModel();
        }

        int processedCount = 0;
        for (int index = effectiveStartIndex; index <= effectiveEndIndex; index++) {

            // Хук для обработки каждой свечи - здесь можно открывать/закрывать позиции и сохранять метрики
            if (!isLife) {
                additionalColumns = captureBacktestPositionMetrics(index, barSeries, shortTradingRecord, longTradingRecord, tradeExecutionModel);
            }

            Bar bar = barSeries.getBar(index);
            DatabaseSnapshot dbRow = snapshotBuilder.build(bar, tagName, schema, additionalColumns, index, isLife);
            dbRows.add(dbRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (processedCount % progressStep == 0 || index == barSeries.getEndIndex()) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.debugf("⏳ Прогресс выполнения: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }
        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(dbRows);

        return shortTradingRecord;
    }

    private TradingRecord getTradingRecord(BarSeries historicalBarSeries, Direction tradeDirection) {
        TradingRecord tradingRecord;

        // Инициализация торговых моделей
        ZeroCostModel transactionCostModel = new ZeroCostModel();
        ZeroCostModel holdingCostModel = new ZeroCostModel();

        tradingRecord = new BaseTradingRecord(
                tradeDirection == Direction.LONG ? Trade.TradeType.BUY : Trade.TradeType.SELL,
                historicalBarSeries.getBeginIndex(),
                historicalBarSeries.getEndIndex(),
                transactionCostModel,
                holdingCostModel
        );
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
     * @param shortTradingRecord       торговый рекорд для управления позициями
     * @param longTradingRecord      торговый рекорд для управления позициями
     * @param tradeExecutionModel модель исполнения сделок
     * @return дополнительные колонки для сохранения в БД (например, позиции, стоп-лосс, тейк-профит)
     */
    protected Map<ColumnTypeMetadata, Num> captureBacktestPositionMetrics(int index,
                                                                          BarSeries historicalBarSeries,
                                                                          TradingRecord shortTradingRecord,
                                                                          TradingRecord longTradingRecord,
                                                                          TradeOnCurrentCloseModel tradeExecutionModel) {

        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        // Торговая логика
        boolean shouldOperate = false;
        Position position = shortTradingRecord.getCurrentPosition();
        // TODO: добавить логику для longTradeEventProcessor, если нужно тестировать обе модели в одном бэктесте

        if (position.isNew()) {
            shouldOperate = !isUnstableAt(index) && shortTradeEventProcessor.shouldMarketEnter(index, shortTradingRecord, false);
        } else if (position.isOpened()) {
            shouldOperate = !isUnstableAt(index) && shortTradeEventProcessor.shouldMarketExit(index, shortTradingRecord, false);
        }

        if (shouldOperate) {
            tradeExecutionModel.execute(index, shortTradingRecord, historicalBarSeries, historicalBarSeries.numFactory().one());
        }

        // Обновление дополнительных колонок
        if (position.isOpened()) {
            Num netPrice = position.getEntry().getNetPrice();
            Num stopLoss = netPrice.multipliedBy(ONE.plus(shortTradeEventProcessor.getStopLossPercentage().dividedBy(HUNDRED)));
            Num takeProfit = netPrice.multipliedBy(ONE.minus(shortTradeEventProcessor.getTakeProfitPercentage().dividedBy(HUNDRED)));

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
