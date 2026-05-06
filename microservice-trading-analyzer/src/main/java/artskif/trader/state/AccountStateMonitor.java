package artskif.trader.state;

import artskif.trader.broker.BrokerConfig;
import artskif.trader.broker.client.TradingExecutionException;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.entity.PendingOrder;
import artskif.trader.entity.Position;
import artskif.trader.mapper.PendingOrderMapper;
import artskif.trader.mapper.PositionMapper;
import artskif.trader.repository.PendingOrderRepository;
import artskif.trader.repository.PositionRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Inject
    PositionRepository positionRepository;

    @Inject
    PositionMapper positionMapper;

    @Inject
    BrokerConfig brokerConfig;

    private final AtomicReference<AccountStateSnapshot> currentSnapshot = new AtomicReference<>();

    /**
     * Флаг актуальности снимка состояния.
     * {@code true} — последний сбор данных прошёл успешно, снимок можно использовать.
     * {@code false} — произошла критическая ошибка (TradingExecutionException), снимок устарел или недостоверен.
     */
    private final AtomicBoolean snapshotHealthy = new AtomicBoolean(false);

    /**
     * Получить последний снимок состояния аккаунта
     *
     * @return последний снимок состояния или null, если данные еще не собраны
     */
    public AccountStateSnapshot getCurrentSnapshot() {
        return currentSnapshot.get();
    }

    /**
     * Проверить, является ли текущий снимок актуальным и достоверным.
     *
     * @return {@code true}, если последний сбор данных завершился успешно;
     *         {@code false}, если произошла ошибка {@link TradingExecutionException}
     *         и снимок больше нельзя использовать
     */
    public boolean isSnapshotHealthy() {
        return snapshotHealthy.get();
    }

    /**
     * Метод вызывается для сбора данных о состоянии аккаунта
     */
    @Scheduled(delay = 1, delayUnit = TimeUnit.SECONDS, every = "15s")
    void collectAccountState() {
        if (!brokerConfig.isTradingEnabled()) {
            log.debug("⏸️ Сбор данных о состоянии аккаунта отключён (broker.trading-enabled=false)");
            return;
        }

        log.debug("📊 Начинается сбор данных о состоянии аккаунта...");

        try {
            // Получаем список всех активных алго-ордеров (null означает все инструменты)
            List<Map<String, Object>> pendingOrdersData = tradingExecutorService.getPendingAlgoOrders("BTC-USDT", "conditional");

            // Преобразуем в Entity (исключаем TP/Limit ордера с isTpLimit=true)
            List<PendingOrder> pendingOrders = pendingOrdersData.stream()
                    .filter(order -> !"true".equalsIgnoreCase(String.valueOf(order.get("isTpLimit"))))
                    .map(data -> pendingOrderMapper.mapToEntity(data, "1m"))
                    .collect(Collectors.toList());

            // Сохраняем ордера в БД
            savePendingOrders(pendingOrders);

            // Получаем список всех открытых позиций (null означает все инструменты)
            List<Map<String, Object>> positionsData = tradingExecutorService.getPositions("BTC-USDT");

            // Преобразуем в Entity
            List<Position> positions = positionsData.stream()
                    .map(data -> positionMapper.mapToEntity(data, "1m"))
                    .collect(Collectors.toList());

            // Сохраняем позиции в БД
            saveLivePositions(positions);

            // Создаем снимок состояния
            AccountStateSnapshot snapshot = new AccountStateSnapshot(
                    Instant.now(),
                    pendingOrders,
                    positions
            );

            // Сохраняем снимок и поднимаем флаг актуальности
            currentSnapshot.set(snapshot);
            snapshotHealthy.set(true);

            log.debug("✅ Снимок состояния аккаунта успешно обновлен: {}", snapshot);

        } catch (TradingExecutionException e) {
            // Критическая ошибка биржевого взаимодействия — снимок больше нельзя использовать
            snapshotHealthy.set(false);
            log.error("❌ Ошибка торгового исполнения при сборе данных о состоянии аккаунта (код: {}): снимок помечен как недостоверный",
                    e.getErrorCode(), e);
        } catch (Exception e) {
            snapshotHealthy.set(false);
            log.error("❌ Ошибка при сборе данных о состоянии аккаунта: снимок помечен как недостоверный", e);
        }
    }

    /**
     * Сохраняет снимок активных ордеров в БД по уникальному ключу (ts, tf).
     * Каждый вызов создаёт новую запись для текущей временной метки.
     */
    private void savePendingOrders(List<PendingOrder> orders) {
        try {
            pendingOrderRepository.saveAllByTsTf(orders);
            log.debug("✅ Обработано активных ордеров: {}", orders.size());

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении ордеров в БД", e);
        }
    }

    /**
     * Сохраняет список открытых позиций в БД.
     * Каждая позиция upsert-ится по уникальному ключу (ts, tf):
     * если запись уже существует — обновляется, иначе вставляется новая.
     */
    private void saveLivePositions(List<Position> positions) {
        try {
            positionRepository.saveAllByTsTf(positions);
            log.debug("✅ Обработано открытых позиций: {}", positions.size());
        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении позиций в БД", e);
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
