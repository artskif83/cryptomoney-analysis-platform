package artskif.trader.broker.manager;

import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.broker.AbstractTradeEventManager;
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

    @Inject
    public TradeEventManager(TradeEventBus tradeEventBus,
                             TradingExecutorService tradingExecutorService,
                             TradeEventRepository tradeEventRepository,
                             BrokerConfig brokerConfig,
                             AccountStateMonitor accountStateMonitor) {
        super(tradeEventBus, tradingExecutorService, tradeEventRepository, brokerConfig);
        this.accountStateMonitor = accountStateMonitor;
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

        boolean isShort = event.tradeEventData().direction() == Direction.SHORT;
        String dirLabel = isShort ? "ШОРТ" : "ЛОНГ";
        String oppositeLabel = isShort ? "ЛОНГ" : "ШОРТ";

        log.debug("📈 Получен сигнал на открытие {} позиции", dirLabel);

        List<PendingOrder> pendingOrders = snapshot.getPendingOrders();
        List<Position> positions = snapshot.getPositions();

        boolean hasOppositePosition = isShort ? hasLongPosition(positions) : hasShortPosition(positions);
        boolean hasAnyPosition = hasLongPosition(positions) || hasShortPosition(positions);
        boolean hasAnyOrder = pendingOrders != null && !pendingOrders.isEmpty();
        boolean farEnough = false;


        if (hasOppositePosition) {
            log.debug("📊 Есть открытая {} позиция", oppositeLabel);
            farEnough = positions.stream()
                    .filter(p -> isShort ? "long".equalsIgnoreCase(p.posSide) : "short".equalsIgnoreCase(p.posSide))
                    .filter(p -> p.state == artskif.trader.entity.OrderState.LIVE)
                    .filter(p -> p.px != null)
                    .anyMatch(p -> {
                        BigDecimal distancePercent = event.tradeEventData().eventPrice()
                                .subtract(p.px)
                                .abs()
                                .divide(p.px, new MathContext(10, RoundingMode.HALF_UP))
                                .multiply(BigDecimal.valueOf(100));
                        boolean ok = distancePercent.compareTo(
                                BigDecimal.valueOf(brokerConfig.getOrderCancelDistancePercent())) >= 0;
                        log.debug("📏 Расстояние от eventPrice={} до px позиции={}: {}% (минимум {}%) — {}",
                                event.tradeEventData().eventPrice(), p.px, distancePercent,
                                brokerConfig.getOrderCancelDistancePercent(), ok ? "✅ достаточно" : "❌ слишком близко");
                        return ok;
                    });
            if (!farEnough) {
                log.warn("⚠️ Закрытие {} позиции пропущено: eventPrice слишком близко к цене открытия (менее {}%)",
                        oppositeLabel, brokerConfig.getOrderCancelDistancePercent());
            }
        }

        // Если нет ни одной открытой позиции, но есть любые ордера — отменяем все перед открытием нового
        if (!hasAnyPosition && hasAnyOrder) {
            log.debug("🔄 Нет открытых позиций, но есть ордера — отменяем все для переоткрытия с новой ценой");
            for (PendingOrder order : pendingOrders) {
                try {
                    tradingExecutorService.cancelOrders(order.ordId, order.clOrdId);
                    log.debug("🗑️ Ордер отменён: ordId={}, clOrdId={}", order.ordId, order.clOrdId);
                } catch (Exception e) {
                    log.error("❌ Ошибка при отмене ордера ordId={}: {}", order.ordId, e.getMessage());
                }
            }
        }

        if (!hasAnyPosition || (hasOppositePosition && farEnough)) {
            log.debug("📊 Нет открытых позиций или есль давняя противоположная позиция, открываем новый ордер {}", dirLabel);

            // Проверяем лимит убыточных позиций за последние 24 часа по истории из снимка
            long losingCount = snapshot.getPositionsHistory() == null ? 0L :
                    snapshot.getPositionsHistory().stream()
                            .filter(p -> p.realizedPnl != null && p.realizedPnl.compareTo(BigDecimal.ZERO) < 0)
                            .count();
            int maxLosing = brokerConfig.getMaxLosingPositionsPerDay();
            if (losingCount >= maxLosing) {
                log.warn("🚫 Достигнут лимит убыточных позиций за последние 24 часа: {} из {}. Новая позиция не открывается.",
                        losingCount, maxLosing);
                return;
            }

            // Проверяем минимальный интервал между позициями по cTime последней позиции из истории
            if (snapshot.getPositionsHistory() != null && !snapshot.getPositionsHistory().isEmpty()) {
                Optional<Instant> lastPositionTime = snapshot.getPositionsHistory().stream()
                        .map(p -> p.cTime)
                        .filter(Objects::nonNull)
                        .max(Instant::compareTo);

                if (lastPositionTime.isPresent()) {
                    long minutesSinceLast = Duration.between(lastPositionTime.get(), Instant.now()).toMinutes();
                    int minMinutes = brokerConfig.getMinutesBetweenPositions();
                    if (minutesSinceLast < minMinutes) {
                        log.warn("⏳ С момента последней позиции прошло {} мин., минимум {} мин. Новая позиция не открывается.",
                                minutesSinceLast, minMinutes);
                        return;
                    }
                }
            }

            FuturesLimitOrderRequest request = new FuturesLimitOrderRequest(
                    event.instrument().replace("-SWAP", ""),
                    event.tradeEventData().eventPrice(),
                    calculatePositionSize(event.tradeEventData().stopLossPercentage()),
                    event.tradeEventData().stopLossPercentage(),
                    event.tradeEventData().takeProfitPercentage()
            );
            if (isShort) {
                tradingExecutorService.placeFuturesLimitShort(request);
            } else {
                tradingExecutorService.placeFuturesLimitLong(request);
            }

            // Сохраняем событие в БД только после успешного размещения ордера
            try {
                TradeEventEntity entity = new TradeEventEntity(
                        event.tradeEventData().type(),
                        event.tradeEventData().direction(),
                        event.instrument(),
                        event.tradeEventData().eventPrice(),
                        event.tradeEventData().stopLossPercentage(),
                        event.tradeEventData().takeProfitPercentage(),
                        event.tradeEventData().timeframe(),
                        event.tag(),
                        event.timestamp(),
                        event.isTest()
                );
                tradeEventRepository.save(entity);
                log.debug("💾 TradeEvent успешно сохранен в БД с UUID: {}", entity.uuid);
            } catch (Exception e) {
                log.error("❌ Ошибка при сохранении TradeEvent в БД", e);
            }
        } else {
            log.debug("📊 Уже есть открытая позиция в том же направлении, не открываем новую {}", dirLabel);
        }
    }

    /**
     * Рассчитывает размер позиции на основе текущего депозита, процента риска и процента стоп-лосса.
     * <p>
     * Формула:
     * <pre>
     *   riskPerTrade  = deposit * depositRiskPercent / 100
     *   positionSize  = riskPerTrade / stopLossPercentage
     * </pre>
     *
     * @param stopLossPercentage процент стоп-лосса (например, 2.0 для 2%)
     * @return рассчитанный размер позиции
     */
    protected BigDecimal calculatePositionSize(BigDecimal stopLossPercentage) {
        BigDecimal deposit = tradingExecutorService.getUsdtBalance();
        BigDecimal stopLossFraction = stopLossPercentage.divide(
                BigDecimal.valueOf(100),
                new MathContext(10, RoundingMode.HALF_UP)
        );
        BigDecimal riskPerTrade = deposit.multiply(
                BigDecimal.valueOf(brokerConfig.getDepositRiskPercent()).divide(
                        BigDecimal.valueOf(100),
                        new MathContext(10, RoundingMode.HALF_UP)
                )
        );
        BigDecimal positionSize = riskPerTrade.divide(
                stopLossFraction,
                new MathContext(10, RoundingMode.HALF_UP)
        );
        log.info("💰 Расчёт размера позиции: депозит={}, риск={}%, риск на сделку={}, стоп-лосс={}%, размер позиции={}",
                deposit, brokerConfig.getDepositRiskPercent(), riskPerTrade, stopLossPercentage, positionSize);
        return positionSize;
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

