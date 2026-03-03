package artskif.trader.broker.manager;

import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.broker.AbstractTradeEventManager;
import artskif.trader.broker.AccountStateMonitor;
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
import java.util.List;

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

        try {
            // Сохраняем событие в БД
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
            // Продолжаем обработку даже если сохранение не удалось
        }

        // Выполняем торговые действия
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
        boolean hasSamePosition = isShort ? hasShortPosition(positions) : hasLongPosition(positions);

        boolean hasSameOrder = isShort ? hasShortOrder(pendingOrders) : hasLongOrder(pendingOrders);

        if (hasOppositePosition) {
            log.debug("📊 Есть открытая {} позиция", oppositeLabel);
            tradingExecutorService.closeAllPositions(event.instrument());
        }

        if (!hasSamePosition && !hasSameOrder) {
            log.debug("📊 Нет открытых {} позиций и ордеров, открываем новую", dirLabel);
            FuturesLimitOrderRequest request = new FuturesLimitOrderRequest(
                    event.instrument(),
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
        } else {
            log.debug("📊 Уже есть открытая {} позиция или ордер, не открываем новую", dirLabel);
        }
    }

    /**
     * Рассчитывает размер позиции на основе текущего депозита, коэффициента риска и процента стоп-лосса.
     * <p>
     * Формула:
     * <pre>
     *   riskPerTrade  = deposit / depositRiskDivisor
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
        BigDecimal riskPerTrade = deposit.divide(
                BigDecimal.valueOf(brokerConfig.getDepositRiskDivisor()),
                new MathContext(10, RoundingMode.HALF_UP)
        );
        BigDecimal positionSize = riskPerTrade.divide(
                stopLossFraction,
                new MathContext(10, RoundingMode.HALF_UP)
        );
        log.info("💰 Расчёт размера позиции: депозит={}, коэффициент={}, риск={}, стоп-лосс={}%, размер позиции={}",
                deposit, brokerConfig.getDepositRiskDivisor(), riskPerTrade, stopLossPercentage, positionSize);
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

    /**
     * Проверяет, есть ли хотя бы один открытый ШОРТ ордер.
     *
     * @param pendingOrders список текущих ожидающих ордеров
     * @return true, если среди ордеров есть хотя бы один в шорт
     */
    private boolean hasShortOrder(List<PendingOrder> pendingOrders) {
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return false;
        }
        return pendingOrders.stream()
                .anyMatch(o -> "short".equalsIgnoreCase(o.posSide));
    }

    /**
     * Проверяет, есть ли хотя бы один открытый ЛОНГ ордер.
     *
     * @param pendingOrders список текущих ожидающих ордеров
     * @return true, если среди ордеров есть хотя бы один в лонг
     */
    private boolean hasLongOrder(List<PendingOrder> pendingOrders) {
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return false;
        }
        return pendingOrders.stream()
                .anyMatch(o -> "long".equalsIgnoreCase(o.posSide));
    }
}

