package artskif.trader.executor.market.okx;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.orders.AccountClient;
import artskif.trader.executor.orders.OrdersClient;
import artskif.trader.executor.common.Symbol;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OkxOrderApiClient extends OkxApiClient implements OrdersClient {

    private static final Logger log = LoggerFactory.getLogger(OkxOrderApiClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final AccountClient accountClient;

    // основной прод-конструктор (через Spring)
    @Autowired
    public OkxOrderApiClient(OkxConfig config, AccountClient accountClient) {
        super(config.getRestApiUrl(), config.getApiKey(), config.getApiSecret(), config.getPassphrase());
        this.accountClient = accountClient;
    }

    // доп. конструктор для тестов (без Spring)
    public OkxOrderApiClient(String restApiUrl,
                             String apiKey,
                             String apiSecret,
                             String passphrase,
                             OkHttpClient httpClient,
                             AccountClient accountClient) {
        super(restApiUrl, apiKey, apiSecret, passphrase, httpClient);
        this.accountClient = accountClient;
    }

    // ==== ExchangeClient ====

    /**
     * Покупка по рынку на спотовом рынке.
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в квотируемой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    @Override
    public OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) {
        // Получаем баланс квотируемой валюты (например, USDT)
        BigDecimal quoteBalance = accountClient.getCurrencyBalance(symbol.quote());
        if (quoteBalance == null || quoteBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("❌ Недостаточный баланс {} для покупки", symbol.quote());
            return null;
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = quoteBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.info("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.quote(), quoteBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "buy", orderSize, true);
        log.info("📊 Результат покупки: {}", result);
        return result;
    }

    /**
     * Продажа по рынку на спотовом рынке.
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    @Override
    public OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) {
        // Получаем баланс базовой валюты (например, BTC)
        BigDecimal baseBalance = accountClient.getCurrencyBalance(symbol.base());
        if (baseBalance == null || baseBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("❌ Недостаточный баланс {} для продажи", symbol.base());
            return null;
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = baseBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.info("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.base(), baseBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "sell", orderSize, false);
        log.info("📊 Результат продажи: {}", result);
        return result;
    }

    /**
     * Получает текущую цену символа в квотируемой валюте.
     * @param symbol Торговая пара
     * @return Текущая цена символа или null в случае ошибки
     */
    @Override
    public BigDecimal getCurrentPrice(Symbol symbol) {
        final String instId = symbol.base() + "-" + symbol.quote();

        try {
            String endpoint = "/api/v5/market/ticker?instId=" + instId;
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить текущую цену для {}. {}", instId, getErrorMessage(response));
                return null;
            }

            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    // Используем last price как текущую цену
                    Object lastPrice = m.get("last");
                    if (lastPrice != null) {
                        BigDecimal price = parseBigDec(lastPrice);
                        log.debug("💹 Текущая цена для {}: {}", instId, price);
                        return price;
                    }
                }
            }

            log.warn("⚠️ Не удалось извлечь цену из ответа для {}", instId);
            return null;
        } catch (Exception e) {
            log.error("❌ Ошибка получения текущей цены для {}: {}", instId, e.getMessage(), e);
            return null;
        }
    }

    // ==== Futures Limit Orders ====

    /**
     * Размещает лимитный лонг-ордер на фьючерсном рынке.
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @param takeProfitPercent Процент отклонения для тейк-профита (например, 5.0 для 5%)
     * @return Результат размещения ордера
     */
    @Override
    public OrderExecutionResult placeFuturesLimitLong(Symbol symbol, BigDecimal limitPrice, BigDecimal positionSizeUsdt,
                                                      BigDecimal stopLossPercent, BigDecimal takeProfitPercent) {
        return placeFuturesLimit(symbol, "buy", limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
    }

    /**
     * Размещает лимитный шорт-ордер на фьючерсном рынке.
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @param takeProfitPercent Процент отклонения для тейк-профита (например, 5.0 для 5%)
     * @return Результат размещения ордера
     */
    @Override
    public OrderExecutionResult placeFuturesLimitShort(Symbol symbol, BigDecimal limitPrice, BigDecimal positionSizeUsdt,
                                                       BigDecimal stopLossPercent, BigDecimal takeProfitPercent) {
        return placeFuturesLimit(symbol, "sell", limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
    }

    /**
     * Размещает лимитный ордер на фьючерсном рынке с поддержкой stop-loss и take-profit.
     * @param symbol Торговая пара
     * @param side "buy" для лонг, "sell" для шорт
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса
     * @param takeProfitPercent Процент отклонения для тейк-профита
     * @return Результат размещения ордера
     */
    private OrderExecutionResult placeFuturesLimit(Symbol symbol, String side, BigDecimal limitPrice,
                                                   BigDecimal positionSizeUsdt,
                                                   BigDecimal stopLossPercent, BigDecimal takeProfitPercent) {
        final String instId = symbol.base() + "-" + symbol.quote() + "-SWAP";
        final String clientId = UUID.randomUUID().toString().replace("-", "");

        try {
            // 1. Получаем параметры инструмента (ctVal и lotSz)
            Map<String, Object> instrumentInfo = getInstrumentInfo(instId);
            if (instrumentInfo == null) {
                log.error("❌ Не удалось получить параметры инструмента {}", instId);
                return null;
            }

            BigDecimal ctVal = parseBigDec(instrumentInfo.get("ctVal"));
            BigDecimal lotSz = parseBigDec(instrumentInfo.get("lotSz"));

            if (ctVal == null || lotSz == null) {
                log.error("❌ Некорректные параметры инструмента: ctVal={}, lotSz={}", ctVal, lotSz);
                return null;
            }

            log.info("📊 Параметры инструмента {}: ctVal={}, lotSz={}", instId, ctVal, lotSz);

            // 2. Вычисляем размер позиции в контрактах
            // Сначала вычисляем объем в базовой валюте (BTC)
            BigDecimal volumeInBase = positionSizeUsdt.divide(limitPrice, 8, RoundingMode.DOWN);

            // Затем конвертируем в количество контрактов
            BigDecimal contractsRaw = volumeInBase.divide(ctVal, 8, RoundingMode.DOWN);

            // Округляем вниз до кратного lotSz
            BigDecimal contractSize = contractsRaw.divide(lotSz, 0, RoundingMode.DOWN).multiply(lotSz);

            if (contractSize.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Размер позиции слишком мал. volumeInBase={}, contractsRaw={}, contractSize={}",
                        volumeInBase, contractsRaw, contractSize);
                return null;
            }

            log.info("🎯 Размещение фьючерсного {} ордера: instId={}, price={}, size={} контрактов (volumeInBase={}, lotSz={})",
                    side, instId, limitPrice, contractSize, volumeInBase, lotSz);

            // 3. Вычисляем цену stop-loss
            BigDecimal stopLossPrice;

            if ("buy".equals(side)) {
                // Для лонга: SL ниже цены входа
                stopLossPrice = limitPrice.multiply(
                        BigDecimal.ONE.subtract(stopLossPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP))
                );
            } else {
                // Для шорта: SL выше цены входа
                stopLossPrice = limitPrice.multiply(
                        BigDecimal.ONE.add(stopLossPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP))
                );
            }

            log.info("💰 Цены: Entry={}, SL={}", limitPrice, stopLossPrice);

            // 4. Вычисляем цены для 3 уровней TP
            BigDecimal[] tpPercentages = {
                takeProfitPercent.multiply(BigDecimal.valueOf(0.5)),  // TP1: 50% от целевого профита
                takeProfitPercent,                                      // TP2: 100% от целевого профита
                takeProfitPercent.multiply(BigDecimal.valueOf(1.5))   // TP3: 150% от целевого профита
            };

            BigDecimal[] sizePercentages = {
                BigDecimal.valueOf(0.5),   // TP1: 50% позиции
                BigDecimal.valueOf(0.3),   // TP2: 30% позиции
                BigDecimal.valueOf(0.2)    // TP3: 20% позиции
            };

            // 5. Формируем массив attachAlgoOrds со всеми SL и TP
            List<Map<String, Object>> attachAlgoOrds = new ArrayList<>();

            // 5.1. Добавляем Stop-Loss ордер
            Map<String, Object> slOrder = new LinkedHashMap<>();
            slOrder.put("slTriggerPxType", "last");  // триггер по последней цене для SL
            slOrder.put("slTriggerPx", stopLossPrice.stripTrailingZeros().toPlainString());
            slOrder.put("slOrdPx", "-1");  // market order при срабатывании SL
            attachAlgoOrds.add(slOrder);

            log.info("🛡️ Добавлен SL ордер: triggerPx={}, sz={}", stopLossPrice, contractSize);

            // 5.2. Добавляем 3 Take-Profit ордера
            BigDecimal totalTpSize = BigDecimal.ZERO;
            for (int i = 0; i < 3; i++) {
                BigDecimal tpPrice = calculateTakeProfitPrice(limitPrice, tpPercentages[i], side);

                // Вычисляем размер с учетом lotSz
                BigDecimal tpSize = contractSize
                        .multiply(sizePercentages[i])
                        .divide(lotSz, 0, RoundingMode.DOWN)
                        .multiply(lotSz);

                // Проверяем, что размер не нулевой
                if (tpSize.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("⚠️ TP{}: размер слишком мал после округления, используем минимальный lotSz", i + 1);
                    tpSize = lotSz;
                }

                totalTpSize = totalTpSize.add(tpSize);

                Map<String, Object> tpOrder = new LinkedHashMap<>();
                tpOrder.put("tpTriggerPxType", "last");  // триггер по последней цене для TP
                tpOrder.put("tpTriggerPx", tpPrice.stripTrailingZeros().toPlainString());
                tpOrder.put("tpOrdPx", "-1");  // market order при срабатывании TP
                tpOrder.put("sz", tpSize.stripTrailingZeros().toPlainString());  // размер конкретного TP
                attachAlgoOrds.add(tpOrder);

                log.info("🎯 Добавлен TP{} ордер: triggerPx={}, sz={} ({}% от позиции)",
                        i + 1, tpPrice, tpSize, sizePercentages[i].multiply(BigDecimal.valueOf(100)));
            }

            // 5.3. Проверяем, что сумма TP равна размеру позиции
            if (totalTpSize.compareTo(contractSize) != 0) {
                log.warn("⚠️ Корректируем размер TP для соответствия позиции: totalTpSize={}, contractSize={}",
                        totalTpSize, contractSize);

                // Корректируем размер последнего TP
                BigDecimal diff = contractSize.subtract(totalTpSize);
                Map<String, Object> lastTp = attachAlgoOrds.getLast();
                BigDecimal lastTpSize = parseBigDec(lastTp.get("sz"));
                BigDecimal correctedSize = lastTpSize.add(diff);

                if (correctedSize.compareTo(BigDecimal.ZERO) > 0) {
                    lastTp.put("sz", correctedSize.stripTrailingZeros().toPlainString());
                    log.info("✅ Скорректирован размер последнего TP: {}", correctedSize);
                } else {
                    log.error("❌ Не удалось скорректировать размер TP");
                }
            }

            log.info("📊 Всего создано {} защитных ордеров: 1 SL + 3 TP", attachAlgoOrds.size());

            // 6. Формируем тело основного лимитного ордера со всеми защитными ордерами (SL + 3 TP)
            Map<String, Object> orderBody = new LinkedHashMap<>();
            orderBody.put("instId", instId);
            orderBody.put("tdMode", "cross");  // cross margin mode
            orderBody.put("side", side);
            orderBody.put("ordType", "limit");
            orderBody.put("px", limitPrice.stripTrailingZeros().toPlainString());
            orderBody.put("sz", contractSize.stripTrailingZeros().toPlainString());
            orderBody.put("clOrdId", clientId);
            orderBody.put("attachAlgoOrds", attachAlgoOrds);  // Привязываем SL + 3 TP сразу

            String requestBody = mapper.writeValueAsString(orderBody);

            log.info("🔐 Размещение ордера с защитой: 1 SL + 3 split TP");

            // 7. Размещаем основной лимитный ордер со всеми защитными ордерами
            Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/order", requestBody);

            if (!isSuccessResponse(response)) {
                throw new RuntimeException("Order placement failed. " + getErrorMessage(response) +
                        (response.containsKey("data") ? ", data: " + response.get("data") : ""));
            }

            // Извлекаем ordId
            String ordId = null;
            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    Object ord = m.get("ordId");
                    if (ord != null) ordId = String.valueOf(ord);
                }
            }

            if (ordId == null) {
                log.error("❌ Ордер размещен, но ordId не получен");
                throw new RuntimeException("Ордер размещен, но ordId не получен: " + safeJson(response));
            }

            log.info("✅ Лимитный фьючерсный ордер размещен с полной защитой (SL + 3 split TP), ordId: {}", ordId);

            // 8. Возвращаем результат основного ордера
            return new OrderExecutionResult(ordId, limitPrice, contractSize);

        } catch (Exception e) {
            log.error("❌ Не удалось разместить фьючерсный лимитный ордер: {}", e.getMessage(), e);
        }
        return null;
    }


    /**
     * Получает параметры инструмента (ctVal, lotSz и т.д.)
     * @param instId Идентификатор инструмента (например, BTC-USDT-SWAP)
     * @return Map с параметрами инструмента или null в случае ошибки
     */
    private Map<String, Object> getInstrumentInfo(String instId) {
        try {
            String endpoint = "/api/v5/public/instruments?instType=SWAP&instId=" + instId;
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить параметры инструмента {}: {}", instId, getErrorMessage(response));
                return null;
            }

            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) m;
                    return result;
                }
            }

            log.warn("⚠️ Не удалось извлечь данные инструмента из ответа для {}", instId);
            return null;
        } catch (Exception e) {
            log.error("❌ Ошибка получения параметров инструмента {}: {}", instId, e.getMessage(), e);
            return null;
        }
    }


    /**
     * Вычисляет цену тейк-профита на основе процента и направления позиции.
     *
     * @param entryPrice Цена входа
     * @param profitPercent Процент профита
     * @param side Направление позиции ("buy" для лонг, "sell" для шорт)
     * @return Цена тейк-профита
     */
    private BigDecimal calculateTakeProfitPrice(BigDecimal entryPrice, BigDecimal profitPercent, String side) {
        BigDecimal percentDiv100 = profitPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        if ("buy".equals(side)) {
            // Для лонга: TP выше цены входа
            return entryPrice.multiply(BigDecimal.ONE.add(percentDiv100));
        } else {
            // Для шорта: TP ниже цены входа
            return entryPrice.multiply(BigDecimal.ONE.subtract(percentDiv100));
        }
    }

    // ==== Основная логика размещения ордеров через REST API ====

    /**
     * Размещает рыночный ордер на спотовом рынке.
     * @param symbol Торговая пара
     * @param side "buy" или "sell"
     * @param size Размер ордера
     * @param isQuoteCurrency true - размер указан в квотируемой валюте, false - в базовой валюте
     * @return Результат исполнения ордера
     */
    private OrderExecutionResult placeSpotMarket(Symbol symbol, String side, BigDecimal size, boolean isQuoteCurrency) {
        final String clientId = UUID.randomUUID().toString().replace("-", "");
        final String instId = symbol.base() + "-" + symbol.quote();

        // Формируем тело запроса для размещения ордера
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("instId", instId);
        orderBody.put("tdMode", "cash");
        orderBody.put("side", side);  // buy | sell
        orderBody.put("ordType", "market");
        orderBody.put("sz", size.stripTrailingZeros().toPlainString());

        // Для покупки указываем размер в квотируемой валюте (например, USDT)
        // Для продажи указываем размер в базовой валюте (например, BTC)
        if (isQuoteCurrency) {
            orderBody.put("tgtCcy", "quote_ccy");  // размер ордера в quote-валюте
        } else {
            orderBody.put("tgtCcy", "base_ccy");  // размер ордера в base-валюте
        }

        orderBody.put("clOrdId", clientId);

        try {
            String requestBody = mapper.writeValueAsString(orderBody);

            // Размещаем ордер
            Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/order", requestBody);

            // Проверяем код ответа
            if (!isSuccessResponse(response)) {
                throw new RuntimeException("Order placement failed. " + getErrorMessage(response));
            }

            // Извлекаем ordId из ответа
            String ordId = null;
            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    Object ord = m.get("ordId");
                    if (ord != null) ordId = String.valueOf(ord);
                }
            }

            if (ordId == null) {
                log.error("❌ Ордер размещен, но ordId не получен: {}", ordId);
                throw new RuntimeException("Ордер размещен, но ordId не получен: " + safeJson(response));
            }

            log.info("✅ Ордер размещен, ordId: {}", ordId);

            // Получаем детали исполнения ордера с retry-логикой
            BigDecimal avgPrice = null;
            BigDecimal execBase = null;

            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                if (attempt > 0) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                Map<String, Object> orderDetails = getOrderDetails(ordId, instId);

                if (orderDetails != null) {
                    String state = String.valueOf(orderDetails.getOrDefault("state", ""));

                    // Проверяем статус ордера
                    if ("filled".equals(state) || "partially_filled".equals(state)) {
                        avgPrice = parseBigDec(orderDetails.get("avgPx"));
                        execBase = parseBigDec(orderDetails.get("accFillSz"));

                        if (avgPrice != null && execBase != null) {
                            log.info("✅ Ордер исполнен: avgPrice={}, execBase={}", avgPrice, execBase);
                            break;
                        }
                    } else if ("canceled".equals(state) || "rejected".equals(state)) {
                        log.error("❌ Ордер был: {}", state + ": " + safeJson(orderDetails));
                        throw new RuntimeException("Ордер был " + state + ": " + safeJson(orderDetails));
                    }
                }
            }

            return new OrderExecutionResult(ordId, avgPrice, execBase);

        } catch (Exception e) {
            log.error("❌ Не удалось разместить ордер на спотовом рынке");
        }
        return null;
    }

    // Получение деталей ордера
    private Map<String, Object> getOrderDetails(String ordId, String instId) {
        try {
            String endpoint = "/api/v5/trade/order?ordId=" + ordId + "&instId=" + instId;
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить информацию о заказе. {}", getErrorMessage(response));
                return null;
            }

            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) m;
                    return result;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("❌ Ошибка получения информации о заказе: {}", e.getMessage(), e);
            return null;
        }
    }
}
