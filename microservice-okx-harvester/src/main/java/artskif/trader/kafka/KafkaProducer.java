package artskif.trader.kafka;

import artskif.trader.websocket.OKXWebSocketClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaProducer {

    private static final Logger LOG = Logger.getLogger(OKXWebSocketClient.class);

    @Inject
    @Channel("producer-1m")
    Emitter<String> emitter1m;

    @Inject
    @Channel("producer-1h")
    Emitter<String> emitter1h;

    @Inject
    @Channel("producer-4h")
    Emitter<String> emitter4h;

    @Inject
    @Channel("producer-1d")
    Emitter<String> emitter1d;

    public void sendMessage(String topic, String message) {
        switch (topic) {
            case "okx-candle-1m" -> emitter1m.send(message);
            case "okx-candle-1h" -> emitter1h.send(message);
            case "okx-candle-4h" -> emitter4h.send(message);
            case "okx-candle-1d" -> emitter1d.send(message);
            default -> LOG.info("⚠️ Неизвестный топик: " + topic);
        }
    }
}
