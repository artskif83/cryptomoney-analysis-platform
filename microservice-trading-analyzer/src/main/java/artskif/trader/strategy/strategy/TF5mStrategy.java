package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF5mSchema;
import artskif.trader.strategy.event.impl.indicator.TrendDownEventProcessor;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.common.TradeEventData;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.*;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.TradeOnCurrentCloseModel;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.*;

@ApplicationScoped
public class TF5mStrategy extends AbstractStrategy {

    private final TradeEventBus tradeEventBus;
    private final AbstractSchema tf5mSchema;


    // Конструктор без параметров для CDI proxy
    protected TF5mStrategy() {
        super(null, null, null, null);
        this.tradeEventBus = null;
        this.tf5mSchema = null;
    }

    @Inject
    public TF5mStrategy(Candle candle,
                        TrendDownEventProcessor eventProcessor,
                        DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService,
                        TF5mSchema TF5mSchema,
                        TradeEventBus tradeEventBus) {
        super(candle, eventProcessor, snapshotBuilder, dataService);
        this.tradeEventBus = tradeEventBus;
        this.tf5mSchema = TF5mSchema;

        // Логирование загруженного EventProcessor
        Log.infof("📦 Загружен EventProcessor: %s", eventProcessor.getClass().getSimpleName());
    }

    @Override
    public String getName() {
        return "TF5m Strategy";
    }

    @Override
    public void onBar(CandlestickDto candle) {

        Optional<TradeEventData> tradeEvent = tradeEventProcessor.detect();

        if (tradeEvent.isPresent()) {
            TradeEventData event = tradeEvent.get();
            Log.infof(
                    "✅ Произошло торговое событие: %s %s (%s) [Режим: %s, Процессор: %s]",
                    event.type(),
                    event.direction(),
                    event.confidence(),
                    tradeEventProcessor.getClass().getSimpleName()
            );

            // Публикуем событие TradeEvent
            tradeEventBus.publish(new TradeEvent(
                    event.type(),
                    candle.getInstrument(),
                    event.direction(),
                    event.confidence(),
                    candle.getTimestamp(),
                    false
            ));
        }

    }

    @Override
    protected CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

    @Override
    protected Integer getUnstableBars() {
        return 14;
    }

    @Override
    public void backtest() {
        Log.info("📋 Начало генерации бектеста для контракта");

        checkColumnsExist();

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();
        int processedCount = 0;
        List<DatabaseSnapshot> dbRows = new ArrayList<>();
        int totalBars = historicalBarSeries.getBarCount();
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        ZeroCostModel transactionCostModel = new ZeroCostModel();
        ZeroCostModel holdingCostModel = new ZeroCostModel();
        TradeOnCurrentCloseModel tradeExecutionModel = new TradeOnCurrentCloseModel();

        TradingRecord tradingRecord = new BaseTradingRecord(Trade.TradeType.SELL, historicalBarSeries.getBeginIndex(), historicalBarSeries.getEndIndex(), transactionCostModel,
                holdingCostModel);

        DecimalNum one = DecimalNum.valueOf(1);
        DecimalNum hundred = DecimalNum.valueOf(100);
        DecimalNum lossPercentage = DecimalNum.valueOf(0.1);
        DecimalNum gainPercentage = DecimalNum.valueOf(1);

        Rule entryRule = tradeEventProcessor.getEntryRule(false);
        Rule exitRule = tradeEventProcessor.getFixedExitRule(false, lossPercentage.bigDecimalValue(), gainPercentage.bigDecimalValue());
        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();

        for (int index = historicalBarSeries.getBeginIndex(); index <= historicalBarSeries.getEndIndex(); index++) {

            boolean shouldOperate = false;

            Position position = tradingRecord.getCurrentPosition();
            if (position.isNew()) {
                shouldOperate = !isUnstableAt(index) && entryRule.isSatisfied(index, tradingRecord);
            } else if (position.isOpened()) {
                shouldOperate = !isUnstableAt(index) && exitRule.isSatisfied(index, tradingRecord);
            }

            if (shouldOperate) {
                tradeExecutionModel.execute(index, tradingRecord, historicalBarSeries, historicalBarSeries.numFactory().one());
            }

            if (position.isOpened()){
                Num netPrice = position.getEntry().getNetPrice();
                Num stopLoss = netPrice.multipliedBy(one.plus(lossPercentage.dividedBy(hundred)));
                Num takeProfit = netPrice.multipliedBy(one.minus(gainPercentage.dividedBy(hundred)));

                additionalColumns.put(PositionColumn.PositionColumnType.POSITION_PRICE_5M, netPrice);
                additionalColumns.put(PositionColumn.PositionColumnType.STOPLOSS_5M, stopLoss);
                additionalColumns.put(PositionColumn.PositionColumnType.TAKEPROFIT_5M, takeProfit);
            } else {
                additionalColumns = new HashMap<>();
            }

            Bar bar = historicalBarSeries.getBar(index);

            DatabaseSnapshot dbRow = snapshotBuilder.build(bar, tf5mSchema, additionalColumns, index, false);

            dbRows.add(dbRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (index > 0 && (index % progressStep == 0 || index == totalBars - 1)) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.infof("⏳ Прогресс тестирования: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }

        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(dbRows);

        Log.infof("✅ Завершено тестирование. Обработано %d свечей", processedCount);
    }

    public void checkColumnsExist() {
        // Проверка что колонки существуют
        for (ContractMetadata metadata : tf5mSchema.getContract().metadata) {
            dataService.ensureColumnExist(metadata.name);
        }
    }

}
