package artskif.trader.restapi.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Общие настройки для всех харвестеров OKX
 */
@ApplicationScoped
@Getter
public class OKXCommonConfig {

    @ConfigProperty(name = "okx.history.baseUrl", defaultValue = "https://www.okx.com")
    String baseUrl;

    @ConfigProperty(name = "okx.history.instId", defaultValue = "BTC-USDT")
    String instId;

    @ConfigProperty(name = "okx.history.limit", defaultValue = "300")
    int limit;

    @ConfigProperty(name = "okx.history.requestPauseMs", defaultValue = "250")
    long requestPauseMs;

    @ConfigProperty(name = "okx.history.maxRetries", defaultValue = "5")
    int maxRetries;

    @ConfigProperty(name = "okx.history.retryBackoffMs", defaultValue = "1000")
    long retryBackoffMs;

    @ConfigProperty(name = "okx.history.pagesLimit", defaultValue = "0")
    int pagesLimit;
}

