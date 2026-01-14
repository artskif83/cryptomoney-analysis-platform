package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.contract.ContractDataService;
import artskif.trader.strategy.contract.schema.AbstractSchema;
import artskif.trader.strategy.contract.schema.impl.Schema4HBase;
import artskif.trader.strategy.contract.schema.impl.Schema5MBase;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.contract.snapshot.ContractSnapshotBuilder;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.event.common.TradeEvent;
import artskif.trader.strategy.event.impl.IndicatorEventModel;
import artskif.trader.strategy.regime.common.MarketRegime;
import artskif.trader.strategy.regime.MarketRegimeModel;
import artskif.trader.strategy.regime.impl.IndicatorMarketRegimeModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class IndicatorStrategy extends AbstractStrategy {

    private final MarketRegimeModel regimeModel;
    private final EventModel eventModel;
    private final Candle candle;
    private final ContractSnapshotBuilder snapshotBuilder;
    private final AbstractSchema schema4hBase;
    private final AbstractSchema schema5mBase;
    private final ContractDataService dataService;

    @Inject
    public IndicatorStrategy(IndicatorMarketRegimeModel regimeModel, IndicatorEventModel eventModel, Candle candle,
                             ContractSnapshotBuilder snapshotBuilder, Schema4HBase schema4hBase, Schema5MBase schema5mBase,
                             ContractDataService dataService) {
        this.regimeModel = regimeModel;
        this.eventModel = eventModel;
        this.candle = candle;
        this.snapshotBuilder = snapshotBuilder;
        this.schema4hBase = schema4hBase;
        this.schema5mBase = schema5mBase;
        this.dataService = dataService;
    }

    @Override
    public String getName() {
        return "Indicator Strategy";
    }

    @Override
    public void onBar(CandlestickDto candle) {

        // 1. Получаем live BarSeries
        BarSeries series = this.candle
                .getInstance(CandleTimeframe.CANDLE_4H)
                .getLiveBarSeries();

        if (series.isEmpty()) {
            Log.debug("Нет HTF баров для анализа");
            return;
        }

        int lastIndex = series.getEndIndex();

        // 2. Собираем snapshot по последнему бару
        ContractSnapshot snapshot =
                snapshotBuilder.build(schema4hBase, lastIndex, true);

        // 3. Определяем режим рынка
        MarketRegime regime =
                regimeModel.classify(snapshot);

        // 4. Получаем live BarSeries для 5m
        BarSeries series5m = this.candle
                .getInstance(CandleTimeframe.CANDLE_5M)
                .getLiveBarSeries();

        if (series5m.isEmpty()) {
            Log.debug("Нет LTF баров для анализа");
            return;
        }

        int lastIndex5m = series5m.getEndIndex();

        // Проверяем, обрабатывали ли мы уже этот бар
        if (lastProcessedBarIndex != null && lastProcessedBarIndex == lastIndex5m) {
            return; // Пропускаем дубликаты
        }


        // 2. Собираем snapshot по последнему бару
        ContractSnapshot snapshot5m =
                snapshotBuilder.build(schema5mBase, lastIndex5m, true);

        Optional<TradeEvent> tradeEvent = Optional.empty();
        if (regime == MarketRegime.TREND_UP) {
            // 4. Детектим событие с учётом режима
            tradeEvent = eventModel.detect(snapshot5m);
            tradeEvent.ifPresent(event -> {
                Log.infof(
                        "✅ TradeEvent: %s %s (%s)",
                        event.type(),
                        event.direction(),
                        event.confidence()
                );
                // дальше: передача в TradeManager / Executor
            });
        }

        if (tradeEvent.isPresent()) {
            // Обновляем индекс
            lastProcessedBarIndex = lastIndex5m;
        }
    }

    @Override
    protected CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

    @Override
    public void generateHistoricalFeatures() {

        // Убеждаемся что контракт инициализирован перед генерацией фич
        if (schema5mBase == null) {
            Log.error("❌ Контракт не инициализирован. Контракт должен быть инициализирован в конструкторе перед генерацией фич.");
            return;
        }

        Log.infof("📋 Начало генерации исторических фич для контракта: %s (hash: %s)", schema5mBase.getName(), schema5mBase.getContractHash());

        // Проверка что колонки существуют
        for (ContractMetadata metadata : schema5mBase.getContract().metadata) {
            dataService.ensureColumnExist(metadata.name, metadata.metadataType);
        }

        BaseBarSeries historicalBarSeries = candle.getInstance(getTimeframe()).getHistoricalBarSeries();
        int processedCount = 0;
        List<ContractSnapshot> futureRows = new ArrayList<>();
        int totalBars = historicalBarSeries.getBarCount();
        int progressStep = Math.max(1, totalBars / 20); // Выводим примерно 20 сообщений (каждые 5%)

        for (int i = historicalBarSeries.getBeginIndex(); i < totalBars + historicalBarSeries.getBeginIndex(); i++) {
            ContractSnapshot featureRow = snapshotBuilder.build(schema5mBase, i, false);

            futureRows.add(featureRow);
            processedCount++;

            // Выводим прогресс каждые progressStep свечей
            if (i > 0 && (i % progressStep == 0 || i == totalBars - 1)) {
                double progressPercent = ((double) processedCount / totalBars) * 100;
                Log.infof("⏳ Прогресс генерации фич: %.1f%% (%d/%d свечей)",
                        progressPercent, processedCount, totalBars);
            }
        }

        // Сохраняем в БД
        dataService.saveContractSnapshotRowsBatch(futureRows);

        Log.infof("✅ Завершена генерация исторических фич для контракта: %s. Обработано %d свечей",
                schema5mBase.getName(), processedCount);
    }

}
