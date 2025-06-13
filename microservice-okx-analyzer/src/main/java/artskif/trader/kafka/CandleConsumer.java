package artskif.trader.kafka;

import artskif.trader.candle.*;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.mapper.CandlestickMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Startup
@ApplicationScoped
public class CandleConsumer {

    @Inject
    Candle1m candle1m;
    @Inject
    Candle1H candle1H;
    @Inject
    Candle4H candle4H;
    @Inject
    Candle1D candle1D;

    @PostConstruct
    void init() {
        System.out.println("🔌 Старт консюмера для обработки свечей");
    }

    @Incoming("candle-1m")
    public void consume1m(String message) {
        candle1m.handleTick(message);
    }

    @Incoming("candle-1h")
    public void consume1H(String message) {
        candle1H.handleTick(message);
    }

    @Incoming("candle-4h")
    public void consume4H(String message) {
        candle4H.handleTick(message);
    }

    @Incoming("candle-1d")
    public void consume1D(String message) {
        candle1D.handleTick(message);
    }
}
