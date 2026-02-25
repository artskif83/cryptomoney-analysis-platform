package artskif.trader.broker;

import artskif.trader.broker.client.TradingExecutorService;
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
            List<Map<String, Object>> pendingOrders = tradingExecutorService.getPendingOrders(null);
            log.debug("📋 Количество активных ордеров: {}", pendingOrders.size());

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
     * Принудительный сбор данных (вне расписания)
     * Полезно для тестирования или немедленного обновления данных
     */
    public void forceCollect() {
        log.info("🔄 Запущен принудительный сбор данных о состоянии аккаунта");
        collectAccountState();
    }
}
