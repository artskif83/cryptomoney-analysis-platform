package artskif.trader.microserviceokxtelegrambot.okx;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletionException;

@RestController
public class OkxController {
    private final OkxOrderService svc;
    public OkxController(OkxOrderService svc) { this.svc = svc; }

    // Пример: GET /okx/place-spot-market
    @GetMapping("/okx/place-spot-market")
    public String placeSpotMarket() {
        try {
            String instId = "BTC-USDT";
            String side = "buy";
            String sz = "10";
            Optional<String> ordId = svc.placeSpotMarketOrderAsync(instId, side, sz).join();
            return "OK, ordId=" + ordId.orElse("<unknown>");
        } catch (CompletionException e) {
            return "ERROR: " + e.getCause().getMessage();
        }
    }
}
