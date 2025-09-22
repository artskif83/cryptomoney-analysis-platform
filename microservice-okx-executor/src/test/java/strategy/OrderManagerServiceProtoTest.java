package strategy;

import artskif.trader.microserviceokxexecutor.orders.OrderManagerService;
import artskif.trader.microserviceokxexecutor.orders.positions.ExchangeClient;
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
    private FakeStore store;
    private FakeExchange exchange;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        rsi = new RsiStrategy(store, new BigDecimal("300"), new BigDecimal("0.01"));
        StrategyRegistry registry = new StrategyRegistry(List.of(rsi));
        exchange = new FakeExchange();
        svc = new OrderManagerService(registry, exchange);
    }

    @Test
    void onSignal_BUY_STRONG_placesSingleAggregatedOrder_andStrategySplitsInto4Units() {
        // STRONG => aggregated baseQty = 0.06000000
        Signal buy = Signal.newBuilder()
                .setId("buy-strong")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.STRONG)
                .setOperation(OperationType.BUY)
                .setPrice(20000.0)
                .build();

        svc.onSignal(buy);

        // Биржа: ровно 1 BUY на 0.06000000
        assertEquals(1, exchange.buyCount);
        bdAssertEq(new BigDecimal("0.06000000"), exchange.lastBuyQty);

        // Стратегия разложила в 4 юнита по 0.01500000
        assertEquals(4, store.usedUnits());
        store.snapshot().forEach(u -> bdAssertEq(new BigDecimal("0.01500000"), u.baseQty));
    }

    @Test
    void onSignal_SELL_sellsOnlyWhenAboveThreshold_andRemovesCheapestUnit() {
        // Предзаполняем: cheap @19000 и exp @20000
        var cheap = new UnitPositionStore.UnitPosition(Symbol.fromProto(BTCUSDT_PROTO), new BigDecimal("19000"), new BigDecimal("0.01111111"), Instant.now());
        var exp   = new UnitPositionStore.UnitPosition(Symbol.fromProto(BTCUSDT_PROTO), new BigDecimal("20000"), new BigDecimal("0.01000000"), Instant.now());
        store.add(exp);
        store.add(cheap);
        assertEquals(2, store.usedUnits());

        // Ниже порога — не продаём
        Signal below = Signal.newBuilder()
                .setId("sell-below")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE)
                .setOperation(OperationType.SELL)
                .setPrice(19100.0) // threshold 19000*1.01 = 19190
                .build();
        svc.onSignal(below);
        assertEquals(0, exchange.sellCount);
        assertEquals(2, store.usedUnits());

        // Выше порога — продаём cheap
        Signal above = Signal.newBuilder()
                .setId("sell-above")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.SMALL)
                .setOperation(OperationType.SELL)
                .setPrice(19200.0)
                .build();
        svc.onSignal(above);

        assertEquals(1, exchange.sellCount);
        bdAssertEq(new BigDecimal("0.01111111"), exchange.lastSellQty);
        assertEquals(1, store.usedUnits());
        bdAssertEq(new BigDecimal("20000"), store.snapshot().get(0).purchasePrice);
    }

    // ---- Fakes ----
    static class FakeStore implements UnitPositionStore {
        private final PriorityQueue<UnitPosition> pq = new PriorityQueue<>(
                Comparator.comparing((UnitPosition u) -> u.purchasePrice).thenComparing(u -> u.purchasedAt)
        );
        @Override public synchronized void add(UnitPosition unit) { pq.add(unit); }
        @Override public synchronized Optional<UnitPosition> peekLowest() { return Optional.ofNullable(pq.peek()); }
        @Override public synchronized Optional<UnitPosition> pollLowest() { return Optional.ofNullable(pq.poll()); }
        @Override public synchronized Optional<UnitPosition> findById(UUID id) {
            for (var u : pq) if (u.id.equals(id)) return Optional.of(u);
            return Optional.empty();
        }
        @Override public synchronized boolean removeById(UUID id) {
            var it = pq.iterator();
            while (it.hasNext()) {
                if (it.next().id.equals(id)) { it.remove(); return true; }
            }
            return false;
        }
        @Override public synchronized int usedUnits() { return pq.size(); }
        @Override public synchronized List<UnitPosition> snapshot() { return new ArrayList<>(pq); }
        @Override public synchronized void clear() { pq.clear(); }
    }

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

    // ---- helpers ----
    private static void bdAssertEq(BigDecimal exp, BigDecimal act) {
        assertEquals(0, exp.compareTo(act),
                "Expected " + exp.toPlainString() + " but was " + act.toPlainString());
    }
}
