package artskif.trader;

import artskif.trader.websocket.OKXWebSocketClient;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@Startup
@ApplicationScoped
public class StartupBean {

    @Inject
    OKXWebSocketClient client;

    @PostConstruct
    void init() {
        client.connect(); // подключение к внешнему WebSocket
        System.out.println("WebSocket client started");
    }
}
