package artskif.trader.buffer;

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

    @Scheduled(delay = 5, delayUnit = TimeUnit.SECONDS, every = "30s")
    void saveAllBuffersPeriodically() {
        candle.getAllInstances().values().stream()
                .filter(instance -> instance.isSaveLiveEnabled() || instance.isSaveHistoricalEnabled())
                .forEach(candleInstance -> {
                    try {
                        candleInstance.saveBuffer();
                    } catch (Exception e) {
                        log.severe("Ошибка при сохранении данных для " + candleInstance.getName() + ": " + e.getMessage());
                    }
                });
    }
}
