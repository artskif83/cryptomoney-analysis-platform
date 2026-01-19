package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.contract.ContractDataService;
import artskif.trader.strategy.contract.schema.AbstractSchema;
import artskif.trader.strategy.contract.schema.impl.Schema4HBase;
import artskif.trader.strategy.contract.schema.impl.Schema5MBase;
import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import artskif.trader.strategy.contract.snapshot.ContractSnapshotBuilder;
import artskif.trader.strategy.event.common.TradeEvent;
import artskif.trader.strategy.event.EventModel;
import artskif.trader.strategy.regime.common.MarketRegime;
import artskif.trader.strategy.regime.impl.IndicatorMarketRegimeModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class IndicatorStrategy extends AbstractStrategy {

    private final AbstractSchema schema4hBase;
    private final AbstractSchema schema5mBase;
    private final BarSeries series;
    private final BarSeries series5m;
    private final TradeEventBus tradeEventBus;

    // Конструктор без параметров для CDI proxy
    protected IndicatorStrategy() {
        super(null, null, null, null, null);
        this.schema4hBase = null;
        this.schema5mBase = null;
        this.series = null;
        this.series5m = null;
        this.tradeEventBus = null;
    }

    @Inject
    public IndicatorStrategy(Candle candle, IndicatorMarketRegimeModel regimeModel,
                             Instance<EventModel> eventModelsInstance,
                             ContractSnapshotBuilder snapshotBuilder, ContractDataService dataService,
                             Schema4HBase schema4hBase, Schema5MBase schema5mBase,
                             TradeEventBus tradeEventBus) {
        // CDI автоматически инжектирует все EventModel (TrendUpEventModel, TrendDownEventModel, FlatEventModel, etc.)
        // Для добавления новой модели просто:
        // 1. Создайте новый класс, реализующий EventModel с аннотацией @ApplicationScoped
        // 2. Модель автоматически будет подхвачена CDI и добавлена в список
        super(candle, regimeModel, 
              StreamSupport.stream(eventModelsInstance.spliterator(), false)
                      .collect(Collectors.toList()), 
              snapshotBuilder, dataService);
        this.schema4hBase = schema4hBase;
        this.schema5mBase = schema5mBase;
        this.series = candle.getInstance(CandleTimeframe.CANDLE_4H).getLiveBarSeries();
        this.series5m = candle.getInstance(CandleTimeframe.CANDLE_5M).getLiveBarSeries();
        this.tradeEventBus = tradeEventBus;

        // Логирование всех найденных EventModel
        Log.infof("📦 Загружено EventModel: %d", eventModels.size());
        eventModels.forEach(model -> 
            Log.infof("  ✓ %s → режим: %s", 
                model.getClass().getSimpleName(), 
                model.getSupportedRegime())
        );
    }

    @Override
    public String getName() {
        return "Indicator Strategy";
    }

    @Override
    public void onBar(CandlestickDto candle) {

        if (series.isEmpty()) {
            Log.debug("Нет HTF баров для анализа");
            return;
        }

        // 1. Определяем режим рынка
        int lastIndex = series.getEndIndex();
        ContractSnapshot snapshot4h =
                snapshotBuilder.build(schema4hBase, lastIndex, true);
        MarketRegime regime =
                regimeModel.classify(snapshot4h);

        // 2. Собираем снапшот для событий рынка в текущем режиме
        if (series5m.isEmpty()) {
            Log.debug("Нет LTF баров для анализа");
            return;
        }
        int lastIndex5m = series5m.getEndIndex();
        // Проверяем, обрабатывали ли мы уже этот бар
        if (lastProcessedBarIndex != null && lastProcessedBarIndex == lastIndex5m) {
            return; // Пропускаем дубликаты
        }
        ContractSnapshot snapshot5m =
                snapshotBuilder.build(schema5mBase, lastIndex5m, true);

        // 3. Проверяем все eventModels и ищем подходящую для текущего режима
        for (EventModel eventModel : eventModels) {
            Optional<TradeEvent> tradeEvent = eventModel.detect(snapshot5m, regime);

            if (tradeEvent.isPresent()) {
                TradeEvent event = tradeEvent.get();
                Log.infof(
                        "✅ TradeEvent: %s %s (%s) [Режим: %s, Модель: %s]",
                        event.type(),
                        event.direction(),
                        event.confidence(),
                        regime,
                        eventModel.getClass().getSimpleName()
                );

                // Публикуем событие TradeEvent
                tradeEventBus.publish(new artskif.trader.events.trade.TradeEvent(
                        event.type(),
                        candle.getInstrument(),
                        event.direction(),
                        event.confidence(),
                        regime,
                        snapshot5m.getTimestamp()
                ));

                // дальше: передача в TradeManager / Executor

                // Обновляем индекс после успешного детектирования
                lastProcessedBarIndex = lastIndex5m;
                break; // Обрабатываем только первое найденное событие
            }
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
