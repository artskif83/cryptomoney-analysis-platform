package artskif.trader.broker;

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

    private final AtomicReference<AccountStateSnapshot> currentSnapshot = new AtomicReference<>();

    /**
     * Получить последний снимок состояния аккаунта
     *
     * @return последний снимок состояния или null, если данные еще не собраны
     */
    public AccountStateSnapshot getCurrentSnapshot() {
        return currentSnapshot.get();
    }

    /**
     * Метод вызывается для сбора данных о состоянии аккаунта
     */
    @Scheduled(delay = 1, delayUnit = TimeUnit.SECONDS, every = "15s")
    void collectAccountState() {
        log.info("📊 Начинается сбор данных о состоянии аккаунта...");

        try {
            // Получаем список всех активных ордеров (null означает все инструменты)
            List<Map<String, Object>> pendingOrdersData = tradingExecutorService.getPendingOrders("BTC-USDT");
            log.debug("📋 Количество активных ордеров: {}", pendingOrdersData.size());

            // Преобразуем в Entity
            List<PendingOrder> pendingOrders = pendingOrdersData.stream()
                    .map(pendingOrderMapper::mapToEntity)
                    .collect(Collectors.toList());

            // Сохраняем ордера в БД
            savePendingOrders(pendingOrders);

            // Получаем список всех открытых позиций (null означает все инструменты)
            List<Map<String, Object>> positionsData = tradingExecutorService.getPositions("BTC-USDT");
            log.debug("📈 Количество открытых позиций: {}", positionsData.size());

            // Преобразуем в Entity
            List<Position> positions = positionsData.stream()
                    .map(positionMapper::mapToEntity)
                    .collect(Collectors.toList());

            // Сохраняем позиции в БД
            saveLivePositions(positions);

            // Вычисляем Unix timestamp 24 часа назад в миллисекундах
            String before24h = String.valueOf(Instant.now().minusSeconds(24 * 60 * 60).toEpochMilli());

            // Получаем историю позиций за последние 24 часа
            List<Map<String, Object>> positionsHistoryData = tradingExecutorService.getPositionsHistory("BTC-USDT", before24h);
            log.debug("📜 Количество записей истории позиций за 24 часа: {}", positionsHistoryData.size());

            // Преобразуем историю позиций в Entity
            List<Position> positionsHistory = positionsHistoryData.stream()
                    .map(positionMapper::mapToClosedEntity)
                    .collect(Collectors.toList());

            // Сохраняем историю позиций в БД со статусом CLOSED
            savePositionsHistory(positionsHistory);

            // Создаем снимок состояния
            AccountStateSnapshot snapshot = new AccountStateSnapshot(
                    Instant.now(),
                    pendingOrders,
                    positions,
                    positionsHistory
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
    private void savePendingOrders(List<PendingOrder> orders) {
        try {
            if (orders.isEmpty()) {
                return;
            }
            // Получаем список текущих ordId для синхронизации
            List<String> currentOrdIds = orders.stream()
                    .map(order -> order.ordId)
                    .collect(Collectors.toList());

            // Сохраняем или обновляем ордера
            pendingOrderRepository.saveAll(orders);
            log.debug("✅ Обработано активных ордеров: {}", orders.size());


            // Помечаем как CLOSED ордера, которых нет в текущем списке
            pendingOrderRepository.markAsClosedNotIn(currentOrdIds);

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении ордеров в БД", e);
        }
    }

    /**
     * Сохраняет список открытых позиций в БД.
     * Удаляет LIVE-позиции, которых нет в текущем списке (синхронизация с биржей).
     */
    private void saveLivePositions(List<Position> positions) {
        try {
            List<String> currentPosIds = positions.stream()
                    .map(p -> p.posId)
                    .toList();

            if (!positions.isEmpty()) {
                positionRepository.saveAll(positions);
                log.debug("✅ Обработано открытых позиций: {}", positions.size());
            }

            positionRepository.deleteLiveNotIn(currentPosIds);
        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении позиций в БД", e);
        }
    }

    /**
     * Сохраняет историю позиций в БД со статусом CLOSED.
     * Не перезаписывает активные (LIVE) позиции историческими данными.
     */
    private void savePositionsHistory(List<Position> positionsHistory) {
        try {
            positionRepository.saveAllHistory(positionsHistory);
            log.debug("✅ Обработано записей истории позиций: {}", positionsHistory.size());
        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении истории позиций в БД", e);
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
