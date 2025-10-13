package artskif.trader.kafka;

import artskif.trader.websocket.OKXWebSocketClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaProducer {

    private static final Logger LOG = Logger.getLogger(KafkaProducer.class);

    // realtime (как у тебя)
    @Inject @Channel("producer-1m")  Emitter<String> emitter1m;
    @Inject @Channel("producer-1h")  Emitter<String> emitter1h;
    @Inject @Channel("producer-4h")  Emitter<String> emitter4h;
    @Inject @Channel("producer-1d")  Emitter<String> emitter1d;

    // history — новые каналы
    @Inject @Channel("producer-1m-history") Emitter<String> emitter1mHist;
    @Inject @Channel("producer-1h-history") Emitter<String> emitter1hHist;
    @Inject @Channel("producer-4h-history") Emitter<String> emitter4hHist;
    @Inject @Channel("producer-1d-history") Emitter<String> emitter1dHist;

    public void sendMessage(String topic, String message) {
        switch (topic) {
            case "okx-candle-1m" -> emitter1m.send(message);
            case "okx-candle-1h" -> emitter1h.send(message);
            case "okx-candle-4h" -> emitter4h.send(message);
            case "okx-candle-1d" -> emitter1d.send(message);
            // history
            case "okx-candle-1m-history" -> emitter1mHist.send(message);
            case "okx-candle-1h-history" -> emitter1hHist.send(message);
            case "okx-candle-4h-history" -> emitter4hHist.send(message);
            case "okx-candle-1d-history" -> emitter1dHist.send(message);
            default -> LOG.info("⚠️ Неизвестный топик: " + topic);
        }
    }
}
