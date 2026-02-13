package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF5mSchema;
import artskif.trader.strategy.event.impl.indicator.TrendDownLevel2EventProcessor;
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

import java.util.Optional;

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
                        TrendDownLevel2EventProcessor eventProcessor,
                        DatabaseSnapshotBuilder snapshotBuilder, StrategyDataService dataService,
                        TF5mSchema tf5mSchema,
                        TradeEventBus tradeEventBus) {
        super(candle, eventProcessor, snapshotBuilder, dataService);
        this.tradeEventBus = tradeEventBus;
        this.tf5mSchema = tf5mSchema;

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
    protected BacktestContext initializeBacktest(BaseBarSeries historicalBarSeries) {
        BacktestContext context = new BacktestContext();

        // Инициализация торговых моделей
        ZeroCostModel transactionCostModel = new ZeroCostModel();
        ZeroCostModel holdingCostModel = new ZeroCostModel();
        TradeOnCurrentCloseModel tradeExecutionModel = new TradeOnCurrentCloseModel();

        TradingRecord tradingRecord = new BaseTradingRecord(
                Trade.TradeType.SELL,
                historicalBarSeries.getBeginIndex(),
                historicalBarSeries.getEndIndex(),
                transactionCostModel,
                holdingCostModel
        );

        // Торговые параметры
        DecimalNum lossPercentage = DecimalNum.valueOf(0.2);
        DecimalNum gainPercentage = DecimalNum.valueOf(5);

        Rule entryRule = tradeEventProcessor.getEntryRule(false);
        Rule exitRule = tradeEventProcessor.getFixedExitRule(false, lossPercentage.bigDecimalValue(), gainPercentage.bigDecimalValue());

        // Сохраняем все необходимые данные в контекст
        context.customData.put("tradingRecord", tradingRecord);
        context.customData.put("tradeExecutionModel", tradeExecutionModel);
        context.customData.put("entryRule", entryRule);
        context.customData.put("exitRule", exitRule);
        context.customData.put("lossPercentage", lossPercentage);
        context.customData.put("gainPercentage", gainPercentage);

        return context;
    }

    @Override
    protected void processBar(int index, BaseBarSeries historicalBarSeries, BacktestContext context) {
        // Извлекаем данные из контекста
        TradingRecord tradingRecord = (TradingRecord) context.customData.get("tradingRecord");
        TradeOnCurrentCloseModel tradeExecutionModel = (TradeOnCurrentCloseModel) context.customData.get("tradeExecutionModel");
        Rule entryRule = (Rule) context.customData.get("entryRule");
        Rule exitRule = (Rule) context.customData.get("exitRule");
        DecimalNum lossPercentage = (DecimalNum) context.customData.get("lossPercentage");
        DecimalNum gainPercentage = (DecimalNum) context.customData.get("gainPercentage");

        DecimalNum one = DecimalNum.valueOf(1);
        DecimalNum hundred = DecimalNum.valueOf(100);

        // Торговая логика
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

        // Обновление дополнительных колонок
        if (position.isOpened()) {
            Num netPrice = position.getEntry().getNetPrice();
            Num stopLoss = netPrice.multipliedBy(one.plus(lossPercentage.dividedBy(hundred)));
            Num takeProfit = netPrice.multipliedBy(one.minus(gainPercentage.dividedBy(hundred)));

            context.additionalColumns.put(PositionColumn.PositionColumnType.POSITION_PRICE_5M, netPrice);
            context.additionalColumns.put(PositionColumn.PositionColumnType.STOPLOSS_5M, stopLoss);
            context.additionalColumns.put(PositionColumn.PositionColumnType.TAKEPROFIT_5M, takeProfit);
        } else {
            context.additionalColumns.clear();
        }
    }

    @Override
    protected AbstractSchema getSchema() {
        return tf5mSchema;
    }


}
