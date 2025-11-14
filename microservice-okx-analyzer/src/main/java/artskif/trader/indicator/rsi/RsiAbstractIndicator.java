package artskif.trader.indicator.rsi;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.candle.AbstractCandle;
import artskif.trader.common.Stage;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.inject.Instance;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class RsiAbstractIndicator extends AbstractIndicator<RsiPoint> {

    protected final TimeSeriesBuffer<RsiPoint> rsiLiveBuffer; // Буфер для хранения актуальных точек RSI
    protected final TimeSeriesBuffer<RsiPoint> rsiHistoricalBuffer; // Буфер для хранения исторических точек RSI

    protected RsiState rsiState; // состояние RSI
    protected RsiPoint lastPoint; // состояние RSI
    protected Integer period; // Период индикатора
    protected BufferRepository<RsiPoint> rsiBufferRepository;
    protected AbstractCandle candle;
    protected Instant bucket;
    protected Instant lastProcessingTime;
    protected List<Stage<RsiPipelineContext>> metrics;

    public RsiAbstractIndicator(AbstractCandle candle, CandleEventBus bus, Instance<Stage<RsiPipelineContext>> metrics, Integer period, BufferRepository<RsiPoint> rsiBufferRepository,
                                int bufferSize, int bufferHistoricalSize) {
        super(bus);
        this.period = period;
        this.rsiBufferRepository = rsiBufferRepository; // Размер буфера для хранения точек индик
        this.rsiLiveBuffer = new TimeSeriesBuffer<>(bufferSize, getCandleTimeframe().getDuration());
        this.rsiHistoricalBuffer = new TimeSeriesBuffer<>(bufferHistoricalSize, getCandleTimeframe().getDuration());
        this.candle = candle;
        this.rsiState = RsiState.empty(period, getCandleTimeframe());
        this.metrics = metrics != null ? metrics.stream()
                .sorted(Comparator.comparingInt(Stage::order))
                .toList() : List.of();
    }

    @Override
    protected void handleEvent(CandleEvent ev) {
        Instant bucket = ev.bucket();

        this.bucket = bucket;
        this.lastProcessingTime = Instant.now();

        TimeSeriesBuffer<CandlestickDto> candleBuffer = candle.getLiveBuffer();
        // Если версия буфера свечей изменилась — пересчитываем индикатор из буфера
        if (rsiLiveBuffer.getLastBucket() == null || (candleBuffer.getLastBucket() != null && rsiLiveBuffer.getLastBucket().isBefore(candleBuffer.getLastBucket()))) {
            log().infof("📥 [%s] версия буфера свечей изменилась — пересчитываем индикатор из буфера", getName());
            recalculateIndicator(candleBuffer);
        }

    }

    private void recalculateIndicator(TimeSeriesBuffer<CandlestickDto> seriesBuffer) {
        if (seriesBuffer != null && !seriesBuffer.isEmpty()) {

            // Фильтруем только подтверждённые свечи и сортируем по времени
            List<Map.Entry<Instant, CandlestickDto>> confirmedCandles = seriesBuffer.getItemsAfter(rsiLiveBuffer.getLastBucket())
                    .entrySet().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getValue().getConfirmed()))
                    .sorted(Map.Entry.comparingByKey())
                    .toList();

            if (!confirmedCandles.isEmpty()) {
                // Выполняем полный пересчет всех значений RSI
                int processedPoints = 0;

                for (Map.Entry<Instant, CandlestickDto> entry : confirmedCandles) {
                    Instant candleBucket = entry.getKey();
                    CandlestickDto candleDto = entry.getValue();

                    // Создаем начальный контекст для пайплайна
                    RsiPipelineContext context = new RsiPipelineContext(
                            rsiState,
                            null,
                            candleBucket,
                            candleDto
                    );

                    // Прогоняем контекст через все метрики (пайплайн)
                    for (Stage<RsiPipelineContext> metric : this.metrics) {
                        context = metric.process(context);
                    }

                    // Сохраняем обновленное состояние из обогащенного контекста
                    rsiState = context.state();

                    // Сохраняем точку в буфер, если она присутствует
                    if (context.point() != null) {
                        rsiLiveBuffer.putItem(context.point().bucket(), context.point());
                        processedPoints++;
                    }
                }

                log().debugf("📥 [%s] RSI индикатор пересчитан из истории свечей. " +
                                "Восстановлено точек: %d, финальное состояние: %s",
                        getName(), processedPoints, rsiState);
                lastPoint = rsiLiveBuffer.getLastItem();
                initSaveBuffer();
            } else {
                log().warnf("📥 [%s] Буфер свечей не содержит подтвержденных данных", getName());
            }
        }
    }
}
