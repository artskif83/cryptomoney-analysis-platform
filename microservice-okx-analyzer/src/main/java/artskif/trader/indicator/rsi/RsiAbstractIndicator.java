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
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RsiAbstractIndicator extends AbstractIndicator<RsiPoint> {

    protected final TimeSeriesBuffer<RsiPoint> rsiLiveBuffer; // Буфер для хранения актуальных точек RSI
    protected final TimeSeriesBuffer<RsiPoint> rsiHistoricalBuffer; // Буфер для хранения исторических точек RSI

    protected AtomicInteger rsiLiveVersion; // версия буфера текущего RSI
    protected AtomicInteger rsiHistoricalVersion; // версия буфера исторического RSI
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
        this.rsiLiveBuffer = new TimeSeriesBuffer<>(bufferSize, getCandleTimeframe().getDuration(), getName() + "-live");
        this.rsiHistoricalBuffer = new TimeSeriesBuffer<>(bufferHistoricalSize, getCandleTimeframe().getDuration(), getName() + "-historical");
        this.candle = candle;
        this.rsiLiveState = RsiState.empty(period, getCandleTimeframe());
        this.rsiHistoricalState = RsiState.empty(period, getCandleTimeframe());
        this.metrics = metrics != null ? metrics.stream()
                .sorted(Comparator.comparingInt(Stage::order))
                .toList() : List.of();
        rsiLiveVersion = new AtomicInteger(0);
        rsiHistoricalVersion = new AtomicInteger(0);
    }

    @Override
    protected void handleHistoryEvent(CandleEvent take) {

        TimeSeriesBuffer<CandlestickDto> candleHistoricalBuffer = candle.getHistoricalBuffer();

        RsiPipelineContext context = recalculateIndicator(rsiHistoricalBuffer, candleHistoricalBuffer, "Исторический буфер", rsiHistoricalState, rsiHistoricalVersion);
        if (context != null) {
            rsiHistoricalState = context.state();
            initSaveBuffer();
        }
        if (rsiHistoricalVersion.get() != candleHistoricalBuffer.getVersion().get()) {
            rsiHistoricalVersion.set(candleHistoricalBuffer.getVersion().get());
        }
    }

    @Override
    protected void handleTickEvent(CandleEvent ev) {
        this.lastProcessingTime = Instant.now();

        TimeSeriesBuffer<CandlestickDto> candleLiveBuffer = candle.getLiveBuffer();


        RsiPipelineContext context = recalculateIndicator(rsiLiveBuffer, candleLiveBuffer, "Актуальный буфер", rsiLiveState, rsiLiveVersion);
        if (context != null) {
            rsiLiveState = context.state();
            initSaveBuffer();
        }
        if (rsiLiveVersion.get() != candleLiveBuffer.getVersion().get()) {
            rsiLiveVersion.set(candleLiveBuffer.getVersion().get());
        }
        lastPoint = rsiLiveBuffer.getLastItem();
    }

    private synchronized RsiPipelineContext recalculateIndicator(TimeSeriesBuffer<RsiPoint> rsiBuffer, TimeSeriesBuffer<CandlestickDto> candleBuffer, String bufferDescription, RsiState rsiState, AtomicInteger version) {

        if (candleBuffer.isEmpty() || candleBuffer.getLastBucket() == null) {
            log().debugf("📥 [%s] %s свечей пустой, пропускаем пересчет исторического RSI индикатора", getName(), bufferDescription);
            return null;
        }
        if (rsiBuffer.size() == rsiBuffer.getMaxSize()) {
            log().debugf("📥 [%s] %s RSI индикатора переполнен. Максимальное количество элементов %s", getName(), bufferDescription, rsiBuffer.getMaxSize());
            return null;
        }

        log().debugf("📥 [%s] %s пересчитывается. RSI буфер: [%s - %s], Candle буфер: [%s - %s]",
                getName(), bufferDescription,
                rsiBuffer.getFirstBucket(), rsiBuffer.getLastBucket(),
                candleBuffer.getFirstBucket(), candleBuffer.getLastBucket());
        if (rsiBuffer.getLastBucket() == null
                || rsiBuffer.getLastBucket().isAfter(candleBuffer.getLastBucket())
                || version.get() != candleBuffer.getVersion().get()) {
            log().debugf("📥 [%s] %s очищаем состояние и пересчитываем с нуля. Причина: rsiLastBucket=%s, candleLastBucket=%s, rsiVersion=%d, candleVersion=%d",
                    getName(), bufferDescription, rsiBuffer.getLastBucket(), candleBuffer.getLastBucket(), version.get(), candleBuffer.getVersion().get());
            rsiState = RsiState.empty(period, getCandleTimeframe());
            rsiBuffer.clear();
        }
        Map<Instant, CandlestickDto> candleItemsBetween = candleBuffer.getItemsBetween(rsiBuffer.getLastBucket(), null);

        log().debugf("📥 [%s] %s пересчитываем для %d элементов", getName(), bufferDescription, candleItemsBetween.size());
        RsiPipelineContext context = null;
        if (!candleItemsBetween.isEmpty()) {

            for (Map.Entry<Instant, CandlestickDto> entry : candleItemsBetween.entrySet()) {
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
            log().debugf("📥 [%s] %s RSI индикатор пересчитан. Финальное состояние %s и буфер %s",
                    getName(), bufferDescription, rsiState, rsiBuffer);
        } else {
            log().warnf("📥 [%s] Буфер свечей не содержит подтвержденных данных", getName());
        }
        return context;
    }
}

