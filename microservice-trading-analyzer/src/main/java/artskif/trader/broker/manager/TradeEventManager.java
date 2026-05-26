package artskif.trader.broker.manager;

import artskif.trader.api.dto.FuturesChaseOrderRequest;
import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.broker.AbstractTradeEventManager;
import artskif.trader.entity.OrderCreationParams;
import artskif.trader.repository.OrderCreationParamsRepository;
import artskif.trader.state.AccountStateMonitor;
import artskif.trader.broker.BrokerConfig;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.entity.PendingOrder;
import artskif.trader.entity.TradeEventEntity;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.repository.TradeEventRepository;
import artskif.trader.entity.Position;
import artskif.trader.strategy.event.common.Direction;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Startup
@ApplicationScoped
@NoArgsConstructor
public class TradeEventManager extends AbstractTradeEventManager {

    private static final Logger log = LoggerFactory.getLogger(TradeEventManager.class);

    protected AccountStateMonitor accountStateMonitor;
    protected OrderCreationParamsRepository orderCreationParamsRepository;

    /**
     * Время последнего успешно размещённого ордера.
     */
    private volatile Instant lastOrderTime = null;

    @Inject
    public TradeEventManager(TradeEventBus tradeEventBus,
                             TradingExecutorService tradingExecutorService,
                             TradeEventRepository tradeEventRepository,
                             BrokerConfig brokerConfig,
                             AccountStateMonitor accountStateMonitor,
                             OrderCreationParamsRepository orderCreationParamsRepository) {
        super(tradeEventBus, tradingExecutorService, tradeEventRepository, brokerConfig);
        this.accountStateMonitor = accountStateMonitor;
        this.orderCreationParamsRepository = orderCreationParamsRepository;
    }

    @Override
    protected void handleTradeEvent(TradeEvent event) {
        log.debug("🔄 Обработка торгового события: {}", event);


        // Выполняем торговые действия
        if (!brokerConfig.isTradingEnabled()) {
            log.warn("🚫 Торговля отключена (broker.trading-enabled=false). Открытие позиции пропущено для события: {}", event);
            return;
        }
        openPosition(event);
    }

    /**
     * Закрывает противоположную позицию (если есть) и открывает новую в заданном направлении.
     */
    private void openPosition(TradeEvent event) {
        var snapshot = accountStateMonitor.getCurrentSnapshot();
        if (snapshot == null || !accountStateMonitor.isSnapshotHealthy()) {
            log.warn("⚠️ Снимок состояния аккаунта недоступен или неактуален, пропускаем торговое действие");
            return;
        }

        Integer trendStrength = event.tradeEventData().trendStrength();
        Integer trendStability = event.tradeEventData().trendStability();
        OrderCreationParams orderCreationParams = orderCreationParamsRepository.findByTrendStrengthAndStability(trendStrength, trendStability);

        log.debug("📋 Параметры создания ордера: {}", orderCreationParams);

        if (orderCreationParams == null) {
            log.warn("⚠️ Параметры создания ордера не найдены для силы тренда: {}", trendStrength);
            return;
        }

        boolean isShort = event.tradeEventData().direction() == Direction.SHORT;
        String dirLabel = isShort ? "ШОРТ" : "ЛОНГ";
        String oppositeLabel = isShort ? "ЛОНГ" : "ШОРТ";

        log.debug("📈 Получен сигнал на открытие {} позиции", dirLabel);

        List<PendingOrder> pendingOrders = snapshot.getPendingOrders();
        List<Position> positions = snapshot.getPositions();

        boolean hasOppositePosition = isShort ? hasLongPosition(positions) : hasShortPosition(positions);
        boolean hasAnyPosition = hasLongPosition(positions) || hasShortPosition(positions);
        boolean hasAnyOrder = pendingOrders != null && !pendingOrders.isEmpty();
        boolean closeOpposite = isShort ? hasOppositePosition && orderCreationParams.closeOppositeLong : hasOppositePosition && orderCreationParams.closeOppositeShort;


        // Закрываем стоп-лосс ордера, которые относятся к несуществующим / противоположным позициям
        if (pendingOrders != null && !pendingOrders.isEmpty()) {
            boolean isLongPosition = hasLongPosition(positions);
            boolean isShortPosition = hasShortPosition(positions);

            for (PendingOrder order : pendingOrders) {
                boolean isLongOrder = "long".equalsIgnoreCase(order.posSide);
                boolean isShortOrder = "short".equalsIgnoreCase(order.posSide);

                boolean shouldCancel = (isLongOrder && isLongPosition) || (isShortOrder && isShortPosition);

                if (shouldCancel) {
                    try {
                        log.info("🗑️ Отмена устаревшего стоп-лосс ордера ordId={} orderSide={}: позиции {} не существует",
                                order.ordId, order.posSide, isShortPosition ? "ШОРТ" : "ЛОНГ");
                        tradingExecutorService.cancelAlgoOrder(order.ordId, order.instId);
                    } catch (Exception e) {
                        log.error("❌ Ошибка при отмене ордера ordId={}: {}", order.ordId, e.getMessage(), e);
                    }
                }
            }
        }

        // Проверяем есть ли противоположная позиция что бы закрывать ее
        if (!hasOppositePosition && (isShort && orderCreationParams.shortOnlyClose || !isShort && orderCreationParams.longOnlyClose)) {
            log.warn("⚠️ Получен сигнал на сокращение(уменьшение) {} позиции, но противоположная позиция {} отсутствует или позиции нет вообще.",
                    dirLabel, oppositeLabel);
            return;
        }

        BigDecimal currentPositionSize = positions.isEmpty() ? BigDecimal.ZERO : positions.getFirst().notionalUsd;


        BigDecimal calculatedPositionSize = calculatePositionSize(accountStateMonitor.getCurrentSnapshot().getTotalEquityInUsdt(),
                isShort ? orderCreationParams.shortDepositRiskPercent : orderCreationParams.longDepositRiskPercent,
                closeOpposite ? currentPositionSize : BigDecimal.ZERO);

        if (calculatedPositionSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ Рассчитанный размер позиции для {} равен нулю или отрицательный ({}), новый ордер не будет открыт.",
                    dirLabel, calculatedPositionSize);
            return;
        }

        // Проверяем минимальный интервал между открытием ордеров
        if (lastOrderTime != null && orderCreationParams.waitMinutes != null && orderCreationParams.waitMinutes > 0
                && !event.isTest()) {
            long minutesSinceLast = Duration.between(lastOrderTime, Instant.now()).toMinutes();
            if (minutesSinceLast < orderCreationParams.waitMinutes) {
                log.warn("⏳ С момента последнего ордера прошло {} мин., минимальный интервал {} мин. Новый ордер не открывается.",
                        minutesSinceLast, orderCreationParams.waitMinutes);
                return;
            }
        }

        if (orderCreationParams.maxPositionSizePercent != null && orderCreationParams.maxPositionSizePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAllowedSize = accountStateMonitor.getCurrentSnapshot().getTotalEquityInUsdt()
                    .multiply(orderCreationParams.maxPositionSizePercent)
                    .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalPositionSize = calculatedPositionSize.add(currentPositionSize);
            boolean closeOnly = isShort ? orderCreationParams.shortOnlyClose : orderCreationParams.longOnlyClose;

            if (!closeOpposite && !hasOppositePosition && totalPositionSize.compareTo(maxAllowedSize) > 0) {
                log.warn("⚠️ Рассчитанный размер позиции ({}) превышает максимальный допустимый размер ({}), установленный параметрами стратегии. Новый ордер не будет открыт.",
                        calculatedPositionSize, maxAllowedSize);
                return;
            }
        }

        FuturesChaseOrderRequest chaseRequest = new FuturesChaseOrderRequest(
                event.instrument().replace("-SWAP", ""),
                calculatedPositionSize,
                orderCreationParams.stopLossDeviationPercent,
                isShort ? orderCreationParams.shortOnlyClose : orderCreationParams.longOnlyClose
        );

        OrderExecutionResult orderExecutionResult;

        if (isShort) {
            orderExecutionResult = tradingExecutorService.placeFuturesChaseShort(chaseRequest);
        } else {
            orderExecutionResult = tradingExecutorService.placeFuturesChaseLong(chaseRequest);
        }

        // Сохраняем событие в БД только после успешного размещения ордера
        if (orderExecutionResult != null && orderExecutionResult.exchangeOrderId() != null && !orderExecutionResult.exchangeOrderId().isBlank()) {
            try {
                TradeEventEntity entity = new TradeEventEntity(
                        event.tradeEventData().type(),
                        event.tradeEventData().direction(),
                        event.instrument(),
                        orderExecutionResult.avgPrice(),
                        null,
                        null,
                        event.tradeEventData().timeframe(),
                        event.tag(),
                        event.timestamp(),
                        event.isTest()
                );
                tradeEventRepository.save(entity);
                lastOrderTime = Instant.now();
                log.debug("💾 TradeEvent успешно сохранен в БД с UUID: {}", entity.uuid);
            } catch (Exception e) {
                log.error("❌ Ошибка при сохранении TradeEvent в БД", e);
            }
        } else {
            log.warn("⚠️ Ордер не был успешно размещён, событие не сохранено в БД. orderExecutionResult={}", orderExecutionResult);
        }
    }


    /**
     * Рассчитывает размер открываемой позиции.
     *
     * @param totalEquity         общий размер депозита в USDT
     * @param riskPercent         процент от депозита, выделяемый на открытие позиции
     * @param currentPositionSize размер текущей позиции, которую необходимо закрыть (0 если нет)
     * @return итоговый размер позиции = (депозит * riskPercent / 100) + currentPositionSize
     */
    protected BigDecimal calculatePositionSize(BigDecimal totalEquity,
                                               BigDecimal riskPercent,
                                               BigDecimal currentPositionSize) {
        if (totalEquity == null || riskPercent == null) {
            log.warn("⚠️ calculatePositionSize: totalEquity или riskPercent равны null");
            return BigDecimal.ZERO;
        }

        BigDecimal riskAmount = totalEquity
                .multiply(riskPercent)
                .divide(BigDecimal.valueOf(100), MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal existing = currentPositionSize != null ? currentPositionSize : BigDecimal.ZERO;
        BigDecimal result = riskAmount.add(existing).setScale(2, RoundingMode.HALF_UP);

        log.debug("💰 calculatePositionSize: totalEquity={}, riskPercent={}, currentPositionSize={} → result={}",
                totalEquity, riskPercent, existing, result);

        return result;
    }

    /**
     * Проверяет, есть ли хотя бы одна открытая ЛОНГ позиция.
     *
     * @param positions список текущих позиций
     * @return true, если среди позиций есть хотя бы одна в лонг
     */
    private boolean hasLongPosition(List<Position> positions) {
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        return positions.stream()
                .anyMatch(p -> "long".equalsIgnoreCase(p.posSide));
    }

    /**
     * Проверяет, есть ли хотя бы одна открытая ШОРТ позиция.
     *
     * @param positions список текущих позиций
     * @return true, если среди позиций есть хотя бы одна в шорт
     */
    private boolean hasShortPosition(List<Position> positions) {
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        return positions.stream()
                .anyMatch(p -> "short".equalsIgnoreCase(p.posSide));
    }
}

