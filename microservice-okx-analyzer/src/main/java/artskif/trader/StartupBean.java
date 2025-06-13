package artskif.trader;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class StartupBean {

    @PostConstruct
    void init() {
        System.out.println("WebSocket client started");
    }
}

