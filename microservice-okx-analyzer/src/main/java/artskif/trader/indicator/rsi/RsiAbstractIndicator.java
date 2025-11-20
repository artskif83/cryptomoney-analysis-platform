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

    protected RsiState rsiLiveState; // состояние RSI
    protected RsiState rsiHistoricalState; // состояние RSI
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
        this.rsiLiveBuffer = new TimeSeriesBuffer<>(bufferSize, getCandleTimeframe().getDuration(), getName()+"-live");
        this.rsiHistoricalBuffer = new TimeSeriesBuffer<>(bufferHistoricalSize, getCandleTimeframe().getDuration(), getName()+"-historical");
        this.candle = candle;
        this.rsiLiveState = RsiState.empty(period, getCandleTimeframe());
        this.rsiHistoricalState = RsiState.empty(period, getCandleTimeframe());
        this.metrics = metrics != null ? metrics.stream()
                .sorted(Comparator.comparingInt(Stage::order))
                .toList() : List.of();
    }

    @Override
    protected void handleHistoryEvent(CandleEvent take) {

        TimeSeriesBuffer<CandlestickDto> historicalBuffer = candle.getHistoricalBuffer();
        // Если версия буфера свечей изменилась — пересчитываем индикатор из буфера
        if (rsiHistoricalBuffer.getFirstBucket() == null || (historicalBuffer.getFirstBucket() != null && rsiHistoricalBuffer.getFirstBucket().isAfter(historicalBuffer.getFirstBucket()))) {
            log().debugf("📥 [%s] Начинаем пересчет исторического RSI индикатора. Пересчет до свечи %s", getName(), rsiHistoricalBuffer.getFirstBucket());
            Map<Instant, CandlestickDto> candleItemsBetween = historicalBuffer.getItemsBetween(null, rsiHistoricalBuffer.getFirstBucket());
            rsiHistoricalState = RsiState.empty(period, getCandleTimeframe());
            RsiPipelineContext context = recalculateIndicator(candleItemsBetween, rsiHistoricalState, rsiHistoricalBuffer);
            if (context != null) {
                rsiHistoricalState = context.state();
                initSaveBuffer();
            }
            log().debugf("📥 [%s] Исторический RSI индикатор пересчитан. Финальное состояние %s и буфер %s",
                    getName(), rsiHistoricalState, rsiHistoricalBuffer);
        }
    }

    @Override
    protected void handleTickEvent(CandleEvent ev) {
        this.bucket = ev.bucket();
        this.lastProcessingTime = Instant.now();

        TimeSeriesBuffer<CandlestickDto> liveBuffer = candle.getLiveBuffer();
        // Если версия буфера свечей изменилась — пересчитываем индикатор из буфера
        if (rsiLiveBuffer.getLastBucket() == null || (liveBuffer.getLastBucket() != null && rsiLiveBuffer.getLastBucket().isBefore(liveBuffer.getLastBucket()))) {
            log().debugf("📥 [%s] Начинаем пересчет актуального RSI индикатора", getName());

            Map<Instant, CandlestickDto> candleItemsBetween = liveBuffer.getItemsBetween(rsiLiveBuffer.getLastBucket(), null);

            RsiPipelineContext context = recalculateIndicator(candleItemsBetween, rsiLiveState, rsiLiveBuffer);
            if (context != null) {
                rsiLiveState = context.state();
                initSaveBuffer();
            }
            lastPoint = rsiLiveBuffer.getLastItem();
            log().debugf("📥 [%s] Актуальный RSI индикатор пересчитан. Финальное состояние %s и буфер %s",
                    getName(), rsiLiveState, rsiLiveBuffer);
        }
    }

    private synchronized RsiPipelineContext recalculateIndicator(Map<Instant, CandlestickDto> candleItems, RsiState rsiState, TimeSeriesBuffer<RsiPoint> rsiBuffer) {
        RsiPipelineContext context = null;
        if (candleItems != null && !candleItems.isEmpty()) {

            for (Map.Entry<Instant, CandlestickDto> entry : candleItems.entrySet()) {
                Instant candleBucket = entry.getKey();
                CandlestickDto candleDto = entry.getValue();

                // Создаем начальный контекст для пайплайна
                context = new RsiPipelineContext(
                        rsiState,
                        null,
                        candleBucket,
                        candleDto
                );

                // Прогоняем контекст через все метрики (пайплайн)
                for (Stage<RsiPipelineContext> metric : this.metrics) {
                    context = metric.process(context);
                }
                rsiState = context.state();
                // Сохраняем точку в буфер, если она присутствует
                if (context.point() != null) {
                    rsiBuffer.putItem(context.point().bucket(), context.point());
                }
            }
        } else {
            log().warnf("📥 [%s] Буфер свечей не содержит подтвержденных данных", getName());
        }
        return context;
    }
}

