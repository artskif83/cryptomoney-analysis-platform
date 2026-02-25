package artskif.trader.broker;

import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.entity.PendingOrder;
import artskif.trader.mapper.PendingOrderMapper;
import artskif.trader.repository.PendingOrderRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Монитор состояния аккаунта
 * Каждую минуту собирает данные о балансе, ордерах и позициях
 */
@Startup
@ApplicationScoped
public class AccountStateMonitor {

    private static final Logger log = LoggerFactory.getLogger(AccountStateMonitor.class);

    @Inject
    TradingExecutorService tradingExecutorService;

    @Inject
    PendingOrderRepository pendingOrderRepository;

    @Inject
    PendingOrderMapper pendingOrderMapper;

    private final AtomicReference<AccountStateSnapshot> currentSnapshot = new AtomicReference<>();

    /**
     * Получить последний снимок состояния аккаунта
     * @return последний снимок состояния или null, если данные еще не собраны
     */
    public AccountStateSnapshot getCurrentSnapshot() {
        return currentSnapshot.get();
    }

    /**
     * Метод вызывается каждую минуту для сбора данных о состоянии аккаунта
     */
    @Scheduled(delay = 1, delayUnit = TimeUnit.SECONDS, every = "60s")
    void collectAccountState() {
        log.info("📊 Начинается сбор данных о состоянии аккаунта...");

        try {
            // Получаем баланс USDT
            BigDecimal balance = tradingExecutorService.getUsdtBalance();
            log.debug("💰 Баланс USDT: {}", balance);

            // Получаем список всех активных ордеров (null означает все инструменты)
            List<Map<String, Object>> pendingOrdersData = tradingExecutorService.getPendingOrders(null);
            log.debug("📋 Количество активных ордеров: {}", pendingOrdersData.size());

            // Преобразуем в Entity
            List<PendingOrder> pendingOrders = pendingOrdersData.stream()
                    .map(pendingOrderMapper::mapToEntity)
                    .collect(Collectors.toList());

            // Сохраняем ордера в БД
            savePendingOrders(pendingOrdersData);

            // Получаем список всех открытых позиций (null означает все инструменты)
            List<Map<String, Object>> positions = tradingExecutorService.getPositions(null);
            log.debug("📈 Количество открытых позиций: {}", positions.size());

            // Создаем снимок состояния
            AccountStateSnapshot snapshot = new AccountStateSnapshot(
                    Instant.now(),
                    balance,
                    pendingOrders,
                    positions
            );

            // Сохраняем снимок
            currentSnapshot.set(snapshot);

            log.info("✅ Снимок состояния аккаунта успешно обновлен: {}", snapshot);

        } catch (Exception e) {
            log.error("❌ Ошибка при сборе данных о состоянии аккаунта", e);
        }
    }

    /**
     * Сохраняет список активных ордеров в БД
     * Помечает как CLOSED ордера, которых больше нет в списке (синхронизация с биржей)
     */
    private void savePendingOrders(List<Map<String, Object>> pendingOrdersData) {
        try {
            // Преобразуем Map в Entity
            List<PendingOrder> orders = pendingOrdersData.stream()
                    .map(pendingOrderMapper::mapToEntity)
                    .collect(Collectors.toList());

            // Получаем список текущих clOrdId для синхронизации
            List<String> currentClOrdIds = orders.stream()
                    .map(order -> order.clOrdId)
                    .collect(Collectors.toList());

            // Сохраняем или обновляем ордера
            if (!orders.isEmpty()) {
                pendingOrderRepository.saveAll(orders);
                log.debug("✅ Обработано активных ордеров: {}", orders.size());
            } else {
                log.debug("📭 Активных ордеров нет");
            }

            // Помечаем как CLOSED ордера, которых нет в текущем списке
            pendingOrderRepository.markAsClosedNotIn(currentClOrdIds);

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении ордеров в БД", e);
        }
    }

    /**
     * Принудительный сбор данных (вне расписания)
     * Полезно для тестирования или немедленного обновления данных
     */
    public void forceCollect() {
        log.info("🔄 Запущен принудительный сбор данных о состоянии аккаунта");
        collectAccountState();
    }
}
