package artskif.trader.state;

import artskif.trader.broker.BrokerConfig;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEventBus;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Startup
@ApplicationScoped
@NoArgsConstructor
public class OrdersStateMonitor extends AbstractOrdersStateMonitor {

    private static final Logger log = LoggerFactory.getLogger(OrdersStateMonitor.class);

    private TradingExecutorService tradingExecutorService;

    @Inject
    public OrdersStateMonitor(CandleEventBus candleEventBus, BrokerConfig brokerConfig,
                              TradingExecutorService tradingExecutorService) {
        super(candleEventBus, brokerConfig);
        this.tradingExecutorService = tradingExecutorService;
    }

    @Override
    protected void handleCandleEvent(CandlestickDto dto) {
        if (!brokerConfig.isTradingEnabled()) {
            log.debug("⏸️ Торговля отключена (trading-enabled=false), пропускаем обработку ордеров");
            return;
        }

        if (dto.getClose() == null) {
            log.debug("⚠️ CandleEvent без цены закрытия, пропускаем: {}", dto);
            return;
        }

        BigDecimal closePrice = dto.getClose();
        double cancelDistancePercent = brokerConfig.getOrderCancelDistancePercent();

        log.debug("🕯️ OrderManager получил свечу: close={}, cancelDistancePct={}%",
                closePrice, cancelDistancePercent);

        List<Map<String, Object>> pendingOrders;
        try {
            pendingOrders = tradingExecutorService.getPendingOrders(null);
        } catch (Exception e) {
            log.error("❌ Ошибка при получении активных ордеров: {}", e.getMessage(), e);
            return;
        }

        if (pendingOrders.isEmpty()) {
            log.debug("📭 OrderManager нет активных ордеров");
            return;
        }

        for (Map<String, Object> order : pendingOrders) {

            String ordId     = String.valueOf(order.getOrDefault("ordId",   ""));
            String clOrdId   = String.valueOf(order.getOrDefault("clOrdId", ""));
            String pxRaw     = String.valueOf(order.getOrDefault("px", ""));
            Object isTpLimit = order.get("isTpLimit");

            if (Boolean.TRUE.equals(isTpLimit) || "true".equalsIgnoreCase(String.valueOf(isTpLimit))) {
                log.debug("⏭️ Ордер ordId={} является TP/Limit ордером (isTpLimit=true), пропускаем", ordId);
                continue;
            }

            if (pxRaw.isEmpty() || pxRaw.equals("null")) {
                log.debug("⚠️ Ордер для проверки {} не имеет цены, пропускаем", ordId);
                continue;
            }

            BigDecimal orderPrice;
            try {
                orderPrice = new BigDecimal(pxRaw);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Не удалось распарсить цену ордера для проверки {}: {}", ordId, pxRaw);
                continue;
            }

            // Расстояние в процентах: |closePrice - orderPrice| / closePrice * 100
            BigDecimal distance = closePrice.subtract(orderPrice).abs()
                    .divide(closePrice, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            log.debug("📏 Проверяем ордер ordId={}, price={}, distance={}%", ordId, orderPrice, distance);

            if (distance.compareTo(BigDecimal.valueOf(cancelDistancePercent)) > 0) {
                log.info("🗑️ Отменяем ордер с неподходящей дистанцией ordId={}, clOrdId={}: distance={}% > {}%",
                        ordId, clOrdId, distance, cancelDistancePercent);
                try {
                    tradingExecutorService.cancelOrders(ordId.isEmpty() ? null : ordId,
                                                       clOrdId.isEmpty() ? null : clOrdId);
                } catch (Exception e) {
                    log.error("❌ Ошибка при отмене ордера ordId={}: {}", ordId, e.getMessage(), e);
                }
            }
        }
    }
}

