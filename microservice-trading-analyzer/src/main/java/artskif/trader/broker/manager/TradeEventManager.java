package artskif.trader.broker.manager;

import artskif.trader.broker.AbstractTradeEventManager;
import artskif.trader.broker.BrokerConfig;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.entity.TradeEventEntity;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.repository.TradeEventRepository;
import artskif.trader.strategy.event.common.Direction;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
@NoArgsConstructor
public class TradeEventManager extends AbstractTradeEventManager {

    private static final Logger log = LoggerFactory.getLogger(TradeEventManager.class);

    @Inject
    public TradeEventManager(TradeEventBus tradeEventBus,
                             TradingExecutorService tradingExecutorService,
                             TradeEventRepository tradeEventRepository,
                             BrokerConfig brokerConfig) {
        super(tradeEventBus, tradingExecutorService, tradeEventRepository, brokerConfig);
    }

    @Override
    protected void handleTradeEvent(TradeEvent event) {
        log.info("🔄 Обработка TradeEvent: {}", event);

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
            log.info("💾 TradeEvent успешно сохранен в БД с UUID: {}", entity.uuid);

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении TradeEvent в БД", e);
            // Продолжаем обработку даже если сохранение не удалось
        }

        // Выполняем торговые действия
        if (event.tradeEventData().direction() == Direction.SHORT) {
            log.info("📈 Получен сигнал на открытие ШОРТ позиции");
            //tradingExecutorService.placeSpotMarketSell(event.instrument(), BigDecimal.valueOf(10));
        }
    }
}

