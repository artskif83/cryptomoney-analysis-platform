package artskif.trader.buffer;

import artskif.trader.candle.AbstractCandle;
import artskif.trader.candle.Candle;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Startup
@ApplicationScoped
public class BufferSaveScheduler {

    private static final Logger log = Logger.getLogger(BufferSaveScheduler.class.getName());

    @Inject
    Candle candle;

    @Scheduled(delay = 1, delayUnit = TimeUnit.SECONDS, every = "1s")
    void saveLiveBuffersPeriodically() {
        candle.getAllInstances().values().stream()
                .filter(AbstractCandle::isSaveLiveEnabled)
                .forEach(candleInstance -> {
                    try {
                        candleInstance.saveLiveBuffer();
                    } catch (Exception e) {
                        log.severe("Ошибка при сохранении актуального буфера для " + candleInstance.getName() + ": " + e.getMessage());
                    }
                });
    }

    @Scheduled(delay = 10, delayUnit = TimeUnit.SECONDS, every = "10s")
    void saveHistoricalBuffersPeriodically() {
        candle.getAllInstances().values().stream()
                .filter(AbstractCandle::isSaveHistoricalEnabled)
                .forEach(candleInstance -> {
                    try {
                        candleInstance.saveHistoricalBuffer();
                    } catch (Exception e) {
                        log.severe("Ошибка при сохранении исторического буфера для " + candleInstance.getName() + ": " + e.getMessage());
                    }
                });
    }
}
