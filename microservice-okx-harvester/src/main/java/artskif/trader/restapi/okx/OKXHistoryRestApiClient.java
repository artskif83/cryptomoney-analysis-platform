package artskif.trader.restapi.okx;

import artskif.trader.restapi.core.CandleRequest;
import artskif.trader.restapi.core.CryptoRestApiClient;
import artskif.trader.restapi.core.RetryableHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * REST API клиент для биржи OKX
 */
public class OKXHistoryRestApiClient implements CryptoRestApiClient<CandleRequest> {
    private static final Logger LOG = Logger.getLogger(OKXHistoryRestApiClient.class);

    private final String baseUrl;
    private final RetryableHttpClient httpClient;

    public OKXHistoryRestApiClient(String baseUrl, RetryableHttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<JsonNode> fetchCandles(CandleRequest request) {
        String url = buildUrl(request);
        LOG.debugf("🌐 OKX API: %s", url);

        Optional<JsonNode> response = httpClient.executeWithRetry(url);
        return response.filter(this::isValidResponse);
    }

    @Override
    public String getProviderName() {
        return "OKX";
    }

    private String buildUrl(CandleRequest request) {
        StringBuilder uri = new StringBuilder(baseUrl)
                .append("/api/v5/market/history-candles")
                .append("?instId=").append(urlEncode(request.getInstId()))
                .append("&bar=").append(urlEncode(request.getTimeframe()))
                .append("&limit=").append(request.getLimit());

        if (request.getAfter() != null) {
            uri.append("&after=").append(request.getAfter());
        }
        if (request.getBefore() != null) {
            uri.append("&before=").append(request.getBefore());
        }

        return uri.toString();
    }

    private boolean isValidResponse(JsonNode root) {
        int code = root.path("code").asInt();
        if (code != 0) {
            LOG.warnf("⚠️ OKX API error: code=%d msg=%s", code, root.path("msg").asText());
            return false;
        }
        return true;
    }

    private String urlEncode(String s) {
        return s.replace(" ", "%20");
    }
}

