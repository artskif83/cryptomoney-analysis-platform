package strategy;

import artskif.trader.microserviceokxexecutor.orders.positions.InMemoryUnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderInstruction;
import artskif.trader.microserviceokxexecutor.orders.positions.UnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.signal.Level;
import artskif.trader.microserviceokxexecutor.orders.signal.Side;
import artskif.trader.microserviceokxexecutor.orders.signal.Signal2;
import artskif.trader.microserviceokxexecutor.orders.signal.Symbol;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.RsiStrategy;
import my.signals.v1.StrategyKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RsiStrategyTest {

    private static final Symbol BTCUSDT = new Symbol("BTC", "USDT");

    private UnitPositionStore store;
    private RsiStrategy strategy;

    @BeforeEach
    void setUp() {
        store = new InMemoryUnitPositionStore();
        // unitValueQuote = 300 USDT на 1 "юнит", profitThreshold = 1%
        strategy = new RsiStrategy(store, bd("300"), bd("0.01"));
    }

    @Test
    void buy_STRONG_returnsOneAggregatedInstruction_andOnExecutedSplitsInto4Units() {
        // STRONG → 4 юнита; perUnitBase = 300 / 20000 = 0.01500000
        // aggregated = 0.06000000
        Signal2 s = new Signal2("s1", BTCUSDT, StrategyKind.ADX_RSI, Level.STRONG, Side.BUY, bd("20000"), Instant.now());

        List<OrderInstruction> instrs = strategy.decide(s);
        assertEquals(1, instrs.size(), "BUY должен вернуть одну агрегированную инструкцию");
        OrderInstruction instr = instrs.get(0);

        assertEquals(Side.BUY, instr.side());
        assertBdEq(bd("0.06000000"), instr.baseQty());

        // эмулируем fill от биржи: исполнено ровно столько, по цене 20000
        OrderExecutionResult fill = new OrderExecutionResult(UUID.randomUUID().toString(), bd("20000"), instr.baseQty());
        strategy.onExecuted(instr, fill);

        // в сторе появилось 4 юнита, суммарный baseQty равен исполненному
        assertEquals(4, store.usedUnits());
        BigDecimal sum = store.snapshot().stream().map(u -> u.baseQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertBdEq(fill.executedBaseQty(), sum);

        // проверим, что каждый юнит 0.01500000
        store.snapshot().forEach(u -> assertBdEq(bd("0.01500000"), u.baseQty));
        // и цена покупки проставилась из avgPrice
        store.snapshot().forEach(u -> assertBdEq(bd("20000"), u.purchasePrice));
    }

    @Test
    void buy_withRemainder_onExecutedSplitsNMinusOneDown_andLastGetsRemainder() {
        // SMALL → 2 юнита
        Signal2 s = new Signal2("s2", BTCUSDT, StrategyKind.ADX_RSI, Level.SMALL, Side.BUY, bd("20000"), Instant.now());
        List<OrderInstruction> instrs = strategy.decide(s);
        OrderInstruction instr = instrs.get(0);

        // подменим fill на слегка "неровный" объём, чтобы был остаток
        BigDecimal executed = bd("0.05000001"); // делим на 2: 0.02500000 и 0.02500001
        OrderExecutionResult fill = new OrderExecutionResult(UUID.randomUUID().toString(), bd("20010"), executed);
        strategy.onExecuted(instr, fill);

        assertEquals(2, store.usedUnits());
        var snap = store.snapshot();
        // первые N-1 юнитов равны per = executed.divide(N, 8, DOWN)
        BigDecimal per = executed.divide(bd("2"), 8, RoundingMode.DOWN); // 0.02500000
        assertBdEq(per, snap.get(0).baseQty);
        // последний — остаток
        BigDecimal last = executed.subtract(per);
        assertBdEq(last, snap.get(1).baseQty);
        // цена покупки — 20010
        snap.forEach(u -> assertBdEq(bd("20010"), u.purchasePrice));
    }

    @Test
    void sell_returnsEmpty_whenNoUnitsOrBelowThreshold() {
        // Нет юнитов — пусто
        Signal2 sellNoUnits = new Signal2("s3", BTCUSDT, StrategyKind.ADX_RSI, Level.MIDDLE, Side.SELL, bd("99999"), Instant.now());
        assertTrue(strategy.decide(sellNoUnits).isEmpty());

        // Добавим юнит, купленный по 19000
        store.add(new UnitPositionStore.UnitPosition(BTCUSDT, bd("19000"), bd("0.01000000"), Instant.now()));

        // Цена ниже порога (+1% → 19190) — инструкций нет
        Signal2 below = new Signal2("s4", BTCUSDT, StrategyKind.ADX_RSI, Level.MIDDLE, Side.SELL, bd("19100"), Instant.now());
        assertTrue(strategy.decide(below).isEmpty());
    }

    @Test
    void sell_returnsInstruction_forCheapestUnit_andOnExecutedRemovesIt() {
        // Дешёвый юнит @19000 и подороже @20000
        var uCheap = new UnitPositionStore.UnitPosition(BTCUSDT, bd("19000"), bd("0.01111111"), Instant.now());
        var uExp   = new UnitPositionStore.UnitPosition(BTCUSDT, bd("20000"), bd("0.01000000"), Instant.now());
        store.add(uExp);
        store.add(uCheap);
        assertEquals(2, store.usedUnits());

        // Текущая цена 19200 >= 19000*1.01=19190 → должен вернуться SELL по дешёвому юниту
        Signal2 s = new Signal2("s5", BTCUSDT, StrategyKind.ADX_RSI, Level.SMALL, Side.SELL, bd("19200"), Instant.now());
        List<OrderInstruction> sellInstrs = strategy.decide(s);
        assertEquals(1, sellInstrs.size());

        OrderInstruction sell = sellInstrs.get(0);
        assertEquals(Side.SELL, sell.side());
        // объём должен совпасть с baseQty дешёвого юнита
        assertBdEq(uCheap.baseQty, sell.baseQty());
        assertNotNull(sell.positionRef());

        // onExecuted(SELL) удаляет этот юнит
        OrderExecutionResult fill = new OrderExecutionResult(UUID.randomUUID().toString(), bd("19200"), sell.baseQty());
        strategy.onExecuted(sell, fill);

        assertEquals(1, store.usedUnits());
        // в сторе остался только дорогой юнит @20000
        var remaining = store.snapshot();
        assertEquals(1, remaining.size());
        assertBdEq(bd("20000"), remaining.get(0).purchasePrice);
    }

    @Test
    void supports_onlyRSI() {
        assertTrue(strategy.supports(StrategyKind.ADX_RSI));
        // на будущее: если появятся другие виды, здесь можно проверить false
    }

    // --- helpers ---
    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
    private static void assertBdEq(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                "Expected " + expected.toPlainString() + " but was " + actual.toPlainString());
    }
}
