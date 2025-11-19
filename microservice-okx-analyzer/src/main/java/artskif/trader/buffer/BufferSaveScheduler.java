package artskif.trader.buffer;

import artskif.trader.common.AbstractTimeSeries;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Startup
@ApplicationScoped
public class BufferSaveScheduler {

    private static final Logger log = Logger.getLogger(BufferSaveScheduler.class.getName());

    @Inject
    Instance<AbstractTimeSeries<?>> timeSeriesInstances;

    @Scheduled(delay = 5, delayUnit = TimeUnit.SECONDS, every = "30s")
    void saveAllBuffersPeriodically() {
        timeSeriesInstances.stream()
                .filter(AbstractTimeSeries::isSaveEnabled)
                .forEach(timeSeries -> {
                    try {
                        //timeSeries.saveBuffer();
                    } catch (Exception e) {
                        log.severe("Ошибка при сохранении данных для " + timeSeries.getName() + ": " + e.getMessage());
                    }
                });
    }
}
