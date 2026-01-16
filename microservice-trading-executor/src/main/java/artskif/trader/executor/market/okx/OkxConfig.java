package artskif.trader.executor.market.okx;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class OkxConfig {

    @Value("${OKX_REST_API:https://www.okx.com}")
    private String restApiUrl;

    @Value("${OKX_API_KEY}")
    private String apiKey;

    @Value("${OKX_API_SECRET}")
    private String apiSecret;

    @Value("${OKX_API_PASSPHRASE}")
    private String passphrase;
}

