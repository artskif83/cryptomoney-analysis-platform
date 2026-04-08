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
    public OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) throws Exception {
        // Получаем баланс квотируемой валюты (например, USDT)
        BigDecimal quoteBalance = accountClient.getCurrencyBalance(symbol.quote());
        if (quoteBalance == null || quoteBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Недостаточный баланс " + symbol.quote() + " для покупки");
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = quoteBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.debug("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.quote(), quoteBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "buy", orderSize, true);
        log.debug("📊 Результат покупки: {}", result);
        return result;
    }

    /**
     * Продажа по рынку на спотовом рынке.
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    @Override
    public OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) throws Exception {
        // Получаем баланс базовой валюты (например, BTC)
        BigDecimal baseBalance = accountClient.getCurrencyBalance(symbol.base());
        if (baseBalance == null || baseBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Недостаточный баланс " + symbol.base() + " для продажи");
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = baseBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.debug("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.base(), baseBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "sell", orderSize, false);
        log.debug("📊 Результат продажи: {}", result);
        return result;
    }

    /**
     * Получает текущую цену символа в квотируемой валюте.
     * @param symbol Торговая пара
     * @return Текущая цена символа или null в случае ошибки
     */
    @Override
    public BigDecimal getCurrentPrice(Symbol symbol) throws Exception {
        final String instId = symbol.base() + "-" + symbol.quote() + "-SWAP";

        String endpoint = "/api/v5/market/ticker?instId=" + instId;
        Map<String, Object> response = executeRestRequest("GET", endpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить текущую цену для " + instId + ". " + getErrorMessage(response));
        }

        if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> m) {
                Object lastPrice = m.get("last");
                if (lastPrice != null) {
                    BigDecimal price = parseBigDec(lastPrice);
                    log.debug("💹 Текущая цена для {}: {}", instId, price);
                    return price;
                }
            }
        }

        throw new RuntimeException("Не удалось извлечь цену из ответа для " + instId);
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
                                                      BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception {
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
                                                       BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception {
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
                                                   BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception {
        final String instId = symbol.base() + "-" + symbol.quote() + "-SWAP";
        final String clientId = UUID.randomUUID().toString().replace("-", "");

        // 1. Получаем параметры инструмента (ctVal и lotSz)
        Map<String, Object> instrumentInfo = getInstrumentInfo(instId);
        if (instrumentInfo == null) {
            throw new RuntimeException("Не удалось получить параметры инструмента " + instId);
        }

        BigDecimal ctVal = parseBigDec(instrumentInfo.get("ctVal"));
        BigDecimal lotSz = parseBigDec(instrumentInfo.get("lotSz"));

        if (ctVal == null || lotSz == null) {
            throw new RuntimeException("Некорректные параметры инструмента: ctVal=" + ctVal + ", lotSz=" + lotSz);
        }

            log.debug("📊 Параметры инструмента {}: ctVal={}, lotSz={}", instId, ctVal, lotSz);

            // 2. Вычисляем размер позиции в контрактах
            // Сначала вычисляем объем в базовой валюте (BTC)
            BigDecimal volumeInBase = positionSizeUsdt.divide(limitPrice, 8, RoundingMode.DOWN);

            // Затем конвертируем в количество контрактов
            BigDecimal contractsRaw = volumeInBase.divide(ctVal, 8, RoundingMode.DOWN);

            // Округляем вниз до кратного lotSz
            BigDecimal contractSize = contractsRaw.divide(lotSz, 0, RoundingMode.DOWN).multiply(lotSz);

        if (contractSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Размер позиции слишком мал: volumeInBase=" + volumeInBase +
                    ", contractsRaw=" + contractsRaw + ", contractSize=" + contractSize);
        }

            log.debug("🎯 Размещение фьючерсного {} ордера: instId={}, price={}, size={} контрактов (volumeInBase={}, lotSz={})",
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

            log.debug("💰 Цены: Entry={}, SL={}", limitPrice, stopLossPrice);

            // 4. Вычисляем цены для 3 уровней TP (20%, 50%, 100% от целевого профита)
            BigDecimal[] tpPercentages = {
                takeProfitPercent.multiply(BigDecimal.valueOf(0.2)),      // TP1: 9% от целевого профита
                takeProfitPercent.multiply(BigDecimal.valueOf(0.5)),      // TP2: 50% от целевого профита
                takeProfitPercent                                         // TP3: 100% от целевого профита
            };

            BigDecimal[] sizePercentages = {
                BigDecimal.valueOf(0.1),   // TP1: 10% позиции
                BigDecimal.valueOf(0.4),   // TP2: 30% позиции
                BigDecimal.valueOf(0.5)    // TP3: 40% позиции
            };

            // 5. Формируем массив attachAlgoOrds со всеми SL и TP
            List<Map<String, Object>> attachAlgoOrds = new ArrayList<>();

            // 5.1. Добавляем Stop-Loss ордер
            Map<String, Object> slOrder = new LinkedHashMap<>();
            slOrder.put("slTriggerPxType", "last");  // триггер по последней цене для SL
            slOrder.put("slTriggerPx", stopLossPrice.stripTrailingZeros().toPlainString());
            slOrder.put("slOrdPx", "-1");  // market order при срабатывании SL
            slOrder.put("amendPxOnTriggerType", "1");  // SL на цену открытия позиции при срабатывании первого TP
            attachAlgoOrds.add(slOrder);

            log.debug("🛡️ Добавлен SL ордер: triggerPx={}, sz={}", stopLossPrice, contractSize);

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
                tpOrder.put("tpOrdPx", tpPrice.stripTrailingZeros().toPlainString());  // market order при срабатывании TP
                tpOrder.put("tpOrdKind", "limit");  // limit order TP
                tpOrder.put("sz", tpSize.stripTrailingZeros().toPlainString());  // размер конкретного TP
                attachAlgoOrds.add(tpOrder);

                log.debug("🎯 Добавлен TP{} ордер: tpOrdPx={}, sz={} ({}% от позиции)",
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
                    log.debug("✅ Скорректирован размер последнего TP: {}", correctedSize);
                } else {
                    log.error("❌ Не удалось скорректировать размер TP");
                }
            }

            log.debug("📊 Всего создано {} защитных ордеров: 1 SL + 3 TP", attachAlgoOrds.size());

            // 6. Формируем тело основного лимитного ордера со всеми защитными ордерами (SL + 3 TP)
            Map<String, Object> orderBody = new LinkedHashMap<>();
            orderBody.put("instId", instId);
            orderBody.put("tdMode", "isolated");  // cross margin mode
            orderBody.put("side", side);
            orderBody.put("ordType", "limit");
            orderBody.put("px", limitPrice.stripTrailingZeros().toPlainString());
            orderBody.put("sz", contractSize.stripTrailingZeros().toPlainString());
            orderBody.put("clOrdId", clientId);
            orderBody.put("attachAlgoOrds", attachAlgoOrds);  // Привязываем SL + 3 TP сразу

            String requestBody = mapper.writeValueAsString(orderBody);

            log.debug("🔐 Размещение ордера с защитой: 1 SL + 3 split TP");

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

            log.debug("✅ Лимитный фьючерсный ордер размещен с полной защитой (SL + 3 split TP), ordId: {}", ordId);

            // 8. Возвращаем результат основного ордера
            return new OrderExecutionResult(ordId, limitPrice, contractSize);
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
    private OrderExecutionResult placeSpotMarket(Symbol symbol, String side, BigDecimal size, boolean isQuoteCurrency) throws Exception {
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
            throw new RuntimeException("Ордер размещен, но ordId не получен: " + safeJson(response));
        }

        log.debug("✅ Ордер размещен, ordId: {}", ordId);

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
                        log.debug("✅ Ордер исполнен: avgPrice={}, execBase={}", avgPrice, execBase);
                        break;
                    }
                } else if ("canceled".equals(state) || "rejected".equals(state)) {
                    throw new RuntimeException("Ордер был " + state + ": " + safeJson(orderDetails));
                }
            }
        }

        return new OrderExecutionResult(ordId, avgPrice, execBase);
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

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров.
     * @return Список активных SWAP ордеров или пустой список в случае ошибки
     */
    @Override
    public List<Map<String, Object>> getPendingOrders() throws Exception {
        return getPendingLimitSwapOrders(null);
    }

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров для указанного инструмента.
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @return Список активных SWAP ордеров или пустой список в случае ошибки
     */
    @Override
    public List<Map<String, Object>> getPendingLimitSwapOrders(String instId) throws Exception {
        String endpoint = "/api/v5/trade/orders-pending?instType=SWAP";

        // Добавляем параметр instId, если он указан
        if (instId != null && !instId.isEmpty()) {
            endpoint += "&instId=" + instId + "-SWAP";
        }

        Map<String, Object> response = executeRestRequest("GET", endpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить список активных SWAP ордеров. " + getErrorMessage(response));
        }

        if (response.containsKey("data") && response.get("data") instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> order = (Map<String, Object>) m;
                    result.add(order);
                }
            }
            String instInfo = instId != null ? " для " + instId : "";
            log.debug("📋 Получено {} активных SWAP ордеров{}", result.size(), instInfo);
            return result;
        }

        log.warn("⚠️ Активные SWAP ордера отсутствуют или данные некорректны");
        return Collections.emptyList();
    }

    /**
     * Отменяет ордера по заданным критериям.
     * <ul>
     *   <li>Оба null — отменяются все активные ордера</li>
     *   <li>Только ordId не null — ищем ордер с совпадающим ordId</li>
     *   <li>Только clOrdId не null — ищем ордер с совпадающим clOrdId</li>
     *   <li>Оба не null — ищем ордер, у которого совпадают оба значения</li>
     * </ul>
     * @param ordId   Опциональный биржевой идентификатор ордера
     * @param clOrdId Опциональный клиентский идентификатор ордера
     * @return true если отмена прошла успешно, false в противном случае
     */
    @Override
    public boolean cancelOrders(String ordId, String clOrdId) throws Exception {
        boolean hasOrdId   = ordId   != null && !ordId.isEmpty();
        boolean hasClOrdId = clOrdId != null && !clOrdId.isEmpty();

        if (!hasOrdId && !hasClOrdId) {
            // Оба null — отменяем все
            return cancelAllPendingOrders();
        }

        // Ищем нужный ордер среди активных
        return cancelOrderByIds(ordId, clOrdId);
    }

    /**
     * Отменяет конкретный ордер, найденный по ordId и/или clOrdId.
     * Если передан только один из параметров, ищем совпадение по нему.
     * Если переданы оба — оба должны совпасть с одним ордером.
     */
    private boolean cancelOrderByIds(String ordId, String clOrdId) throws Exception {
        boolean hasOrdId   = ordId   != null && !ordId.isEmpty();
        boolean hasClOrdId = clOrdId != null && !clOrdId.isEmpty();

        List<Map<String, Object>> pendingOrders = getPendingOrders();

        Map<String, Object> targetOrder = null;
        for (Map<String, Object> order : pendingOrders) {
            String orderOrdId   = String.valueOf(order.getOrDefault("ordId",   ""));
            String orderClOrdId = String.valueOf(order.getOrDefault("clOrdId", ""));

            boolean ordIdMatch   = hasOrdId   && ordId.equals(orderOrdId);
            boolean clOrdIdMatch = hasClOrdId && clOrdId.equals(orderClOrdId);

            boolean matched;
            if (hasOrdId && hasClOrdId) {
                // Оба должны совпасть
                matched = ordIdMatch && clOrdIdMatch;
            } else {
                // Достаточно одного совпадения
                matched = ordIdMatch || clOrdIdMatch;
            }

            if (matched) {
                targetOrder = order;
                break;
            }
        }

        if (targetOrder == null) {
            log.warn("⚠️ Ордер не найден среди активных: ordId={}, clOrdId={}", ordId, clOrdId);
            return false;
        }

        String foundOrdId   = String.valueOf(targetOrder.get("ordId"));
        String foundInstId  = String.valueOf(targetOrder.get("instId"));
        String foundClOrdId = String.valueOf(targetOrder.getOrDefault("clOrdId", ""));

        log.debug("🔍 Найден ордер для отмены: ordId={}, clOrdId={}, instId={}", foundOrdId, foundClOrdId, foundInstId);

        Map<String, Object> cancelBody = new LinkedHashMap<>();
        cancelBody.put("instId", foundInstId);
        cancelBody.put("ordId", foundOrdId);

        String requestBody = mapper.writeValueAsString(cancelBody);
        Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/cancel-order", requestBody);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось отменить ордер ordId=" + foundOrdId +
                    ", clOrdId=" + foundClOrdId + ". " + getErrorMessage(response));
        }

        log.debug("✅ Ордер ordId={}, clOrdId={} успешно отменен", foundOrdId, foundClOrdId);
        return true;
    }

    /**
     * Отменяет все активные ордера.
     * @return true если все ордера успешно отменены, false если хотя бы одна отмена не удалась
     */
    private boolean cancelAllPendingOrders() throws Exception {
        // Получаем список всех активных ордеров
        List<Map<String, Object>> pendingOrders = getPendingOrders();

        if (pendingOrders.isEmpty()) {
            log.debug("ℹ️ Нет активных ордеров для отмены");
            return true;
        }

        log.debug("🔄 Начинаем отмену {} активных ордеров", pendingOrders.size());

        int successCount = 0;
        int failCount = 0;

        // Отменяем каждый ордер
        for (Map<String, Object> order : pendingOrders) {
            String ordId = String.valueOf(order.get("ordId"));
            String instId = String.valueOf(order.get("instId"));
            String clOrdId = String.valueOf(order.getOrDefault("clOrdId", ""));

            try {
                Map<String, Object> cancelBody = new LinkedHashMap<>();
                cancelBody.put("instId", instId);
                cancelBody.put("ordId", ordId);

                String requestBody = mapper.writeValueAsString(cancelBody);
                Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/cancel-order", requestBody);

                if (!isSuccessResponse(response)) {
                    log.error("❌ Не удалось отменить ордер ordId={}, clOrdId={}. {}",
                            ordId, clOrdId, getErrorMessage(response));
                    failCount++;
                } else {
                    log.debug("✅ Ордер ordId={}, clOrdId={} успешно отменен", ordId, clOrdId);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при отмене ордера ordId={}, clOrdId={}: {}",
                        ordId, clOrdId, e.getMessage(), e);
                failCount++;
            }
        }

        log.debug("📊 Отмена завершена: успешно={}, неудачно={}", successCount, failCount);
        return failCount == 0;
    }

    /**
     * Получает список всех открытых позиций.
     * @param instId Опциональный идентификатор инструмента (например, "BTC-USDT-SWAP") для фильтрации позиций
     * @return Список открытых позиций или пустой список в случае ошибки
     */
    @Override
    public List<Map<String, Object>> getPositions(String instId) throws Exception {
        String endpoint = "/api/v5/account/positions?instType=SWAP";

        // Добавляем параметр instId, если он указан
        if (instId != null && !instId.isEmpty()) {
            // Проверяем, есть ли уже суффикс -SWAP
            if (!instId.endsWith("-SWAP")) {
                endpoint += "&instId=" + instId + "-SWAP";
            } else {
                endpoint += "&instId=" + instId;
            }
        }

        Map<String, Object> response = executeRestRequest("GET", endpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить список позиций. " + getErrorMessage(response));
        }

        if (response.containsKey("data") && response.get("data") instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> position = (Map<String, Object>) m;

                    // Фильтруем позиции с ненулевым размером
                    Object posObj = position.get("pos");
                    if (posObj != null) {
                        BigDecimal pos = parseBigDec(posObj);
                        if (pos != null && pos.compareTo(BigDecimal.ZERO) != 0) {
                            result.add(position);
                        }
                    }
                }
            }
            String instInfo = instId != null ? " для " + instId : "";
            log.debug("📋 Получено {} открытых позиций{}", result.size(), instInfo);
            return result;
        }

        log.warn("⚠️ Открытые позиции отсутствуют или данные некорректны");
        return Collections.emptyList();
    }

    /**
     * Закрывает все текущие открытые позиции используя endpoint /api/v5/trade/close-position.
     * @param instId Опциональный идентификатор инструмента для закрытия только конкретной позиции (может быть null для закрытия всех позиций)
     * @return true если все позиции успешно закрыты, false в противном случае
     */
    @Override
    public boolean closeAllPositions(String instId) throws Exception {
        // Получаем список всех открытых позиций
        List<Map<String, Object>> positions = getPositions(instId);

        if (positions.isEmpty()) {
            log.debug("ℹ️ Нет открытых позиций для закрытия");
            return true;
        }

        log.debug("🔄 Начинаем закрытие {} открытых позиций", positions.size());

        int successCount = 0;
        int failCount = 0;

        // Закрываем каждую позицию
        for (Map<String, Object> position : positions) {
            String posInstId = String.valueOf(position.get("instId"));
            String posSide = String.valueOf(position.getOrDefault("posSide", "net"));
            String mgnMode = String.valueOf(position.getOrDefault("mgnMode", "cross"));

            Object posObj = position.get("pos");
            if (posObj == null) {
                log.warn("⚠️ Позиция без размера, пропускаем: {}", posInstId);
                continue;
            }

            BigDecimal posSize = parseBigDec(posObj);
            if (posSize == null || posSize.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("⚠️ Позиция с нулевым размером, пропускаем: {}", posInstId);
                continue;
            }

            log.debug("📍 Закрытие позиции: instId={}, posSide={}, mgnMode={}, size={}",
                    posInstId, posSide, mgnMode, posSize);

            try {
                boolean closed = closePosition(posInstId, posSide, mgnMode);

                if (closed) {
                    log.debug("✅ Позиция {} успешно закрыта", posInstId);
                    successCount++;
                } else {
                    log.error("❌ Не удалось закрыть позицию {}", posInstId);
                    failCount++;
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при закрытии позиции {}: {}", posInstId, e.getMessage(), e);
                failCount++;
            }
        }

        log.debug("📊 Закрытие позиций завершено: успешно={}, неудачно={}", successCount, failCount);
        return failCount == 0;
    }

    /**
     * Закрывает конкретную позицию используя специализированный endpoint /api/v5/trade/close-position.
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param posSide Направление позиции ("long", "short" или "net")
     * @param mgnMode Режим маржи ("cross" или "isolated")
     * @return true если позиция успешно закрыта, false в противном случае
     */
    private boolean closePosition(String instId, String posSide, String mgnMode) throws Exception {
        // Формируем тело запроса для закрытия позиции
        Map<String, Object> closeBody = new LinkedHashMap<>();
        closeBody.put("instId", instId);
        closeBody.put("mgnMode", mgnMode);
        closeBody.put("autoCxl", "true");

        // Для net режима posSide не передаётся, для long/short - передаётся
        if (!"net".equals(posSide)) {
            closeBody.put("posSide", posSide);
        }

        String requestBody = mapper.writeValueAsString(closeBody);

        log.debug("🔐 Закрытие позиции через /api/v5/trade/close-position: instId={}, posSide={}, mgnMode={}",
                instId, posSide, mgnMode);

        Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/close-position", requestBody);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось закрыть позицию. " + getErrorMessage(response));
        }

        log.debug("✅ Позиция {} успешно закрыта через /api/v5/trade/close-position", instId);
        return true;
    }

    /**
     * Получает историю закрытых позиций по инструменту через /api/v5/account/positions-history.
     * instType фиксирован = SWAP, возвращаются все закрытые позиции (type не передаётся — API
     * по умолчанию возвращает все типы закрытия).
     *
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param before Unix timestamp в миллисекундах (строка); возвращаются записи,
     *               обновлённые позже указанного момента (фильтр по полю uTime)
     * @return Список записей истории позиций или null в случае ошибки
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPositionsHistory(String instId, String before) throws Exception {
        // Формируем endpoint с query-параметрами
        StringBuilder endpoint = new StringBuilder("/api/v5/account/positions-history?instType=SWAP");
        if (instId != null && !instId.isBlank()) {
            endpoint.append("&instId=").append(instId).append("-SWAP");
        }
        if (before != null && !before.isBlank()) {
            endpoint.append("&before=").append(before);
        }

        String fullEndpoint = endpoint.toString();
        log.debug("📋 Запрос истории позиций: {}", fullEndpoint);

        Map<String, Object> response = executeRestRequest("GET", fullEndpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить историю позиций. " + getErrorMessage(response));
        }

        Object dataObj = response.get("data");
        if (dataObj instanceof List<?> dataList) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : dataList) {
                if (item instanceof Map<?, ?> entry) {
                    result.add((Map<String, Object>) entry);
                }
            }
            log.debug("📋 История позиций {}: получено {} записей", instId, result.size());
            return result;
        }

        log.warn("⚠️ История позиций не найдена в ответе API для инструмента {}", instId);
        return Collections.emptyList();
    }
}
