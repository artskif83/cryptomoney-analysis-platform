package strategy;

import artskif.trader.microserviceokxexecutor.orders.OrderManagerService;
import artskif.trader.microserviceokxexecutor.orders.positions.ExchangeClient;
import artskif.trader.microserviceokxexecutor.orders.positions.InMemoryUnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxexecutor.orders.positions.UnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.strategy.StrategyRegistry;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.RsiStrategy;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.Symbol;
import my.signals.v1.OperationType;
import my.signals.v1.Signal;
import my.signals.v1.SignalLevel;
import my.signals.v1.StrategyKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagerServiceProtoTest {

    private static final my.signals.v1.Symbol BTCUSDT_PROTO =
            my.signals.v1.Symbol.newBuilder().setBase("BTC").setQuote("USDT").build();

    private OrderManagerService svc;
    private RsiStrategy rsi;
    private InMemoryUnitPositionStore store;
    private FakeExchange exchange;

    @BeforeEach
    void setUp() {
        store = new InMemoryUnitPositionStore();
        rsi = new RsiStrategy(store); // unitValueQuote=5; SELL: SMALL=0, MIDDLE=1, STRONG=2
        StrategyRegistry registry = new StrategyRegistry(List.of(rsi));
        exchange = new FakeExchange();
        svc = new OrderManagerService(registry, exchange);
    }

    @Test
    void onSignal_BUY_STRONG_placesSingleAggregatedOrder_andStrategySplitsInto4Units() {
        // price=20000, unitValueQuote=5 => perUnitBase=0.00025000; STRONG=4 => aggregated=0.00100000
        Signal buy = Signal.newBuilder()
                .setId("buy-strong")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.STRONG)
                .setOperation(OperationType.BUY)
                .setPrice(20000.0)
                .build();

        svc.onSignal(buy);

        // Биржа: ровно 1 BUY на 0.00100000
        assertEquals(1, exchange.buyCount);
        bdAssertEq(new BigDecimal("0.0600000"), exchange.lastBuyQty);

        // Стратегия разложила в 4 юнита по 0.00025000
        assertEquals(4, store.usedUnits());
        store.snapshot().forEach(u -> bdAssertEq(new BigDecimal("0.015000"), u.baseQty));
    }

    @Test
    void onSignal_SELL_sellsOnlyWhenAboveThreshold_andRemovesCheapestUnit() {
        // Предзаполняем: cheap @19000 и exp @20000
        var cheap = new UnitPositionStore.UnitPosition(
                UUID.randomUUID(), Symbol.fromProto(BTCUSDT_PROTO),
                new BigDecimal("19000"), new BigDecimal("0.01111111"), Instant.now()
        );
        var exp = new UnitPositionStore.UnitPosition(
                UUID.randomUUID(), Symbol.fromProto(BTCUSDT_PROTO),
                new BigDecimal("20000"), new BigDecimal("0.01000000"), Instant.now()
        );
        store.add(exp);
        store.add(cheap);
        assertEquals(2, store.usedUnits());

        // Ниже порога (19100 < 19000*1.01=19190) — не продаём
        Signal below = Signal.newBuilder()
                .setId("sell-below")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE) // SMALL больше не продаёт (0)
                .setOperation(OperationType.SELL)
                .setPrice(19100.0)
                .build();
        svc.onSignal(below);
        assertEquals(0, exchange.sellCount);
        assertEquals(2, store.usedUnits());

        // Выше порога — продаём самый дешёвый (cheap)
        Signal above = Signal.newBuilder()
                .setId("sell-above")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE) // MIDDLE=1 юнит
                .setOperation(OperationType.SELL)
                .setPrice(19200.0)
                .build();
        svc.onSignal(above);

        assertEquals(1, exchange.sellCount);
        bdAssertEq(new BigDecimal("0.01111111"), exchange.lastSellQty);
        assertEquals(1, store.usedUnits());
        bdAssertEq(new BigDecimal("20000"), store.snapshot().get(0).purchasePrice);
    }

    // ---- fakes & helpers ----

    static class FakeExchange implements ExchangeClient {
        int buyCount = 0, sellCount = 0;
        BigDecimal lastBuyQty, lastSellQty;

        @Override
        public OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty) {
            buyCount++; lastBuyQty = baseQty;
            return new OrderExecutionResult(UUID.randomUUID().toString(), new BigDecimal("20000"), baseQty);
        }

        @Override
        public OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty) {
            sellCount++; lastSellQty = baseQty;
            return new OrderExecutionResult(UUID.randomUUID().toString(), new BigDecimal("19200"), baseQty);
        }
    }

    private static void bdAssertEq(BigDecimal exp, BigDecimal act) {
        assertEquals(0, exp.compareTo(act),
                "Expected " + exp.toPlainString() + " but was " + act.toPlainString());
    }
}
