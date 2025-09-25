package strategy;

import artskif.trader.microserviceokxexecutor.orders.positions.InMemoryUnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxexecutor.orders.positions.UnitPositionStore;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.RsiStrategy;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.Symbol;
import my.signals.v1.OperationType;
import my.signals.v1.Signal;
import my.signals.v1.SignalLevel;
import my.signals.v1.StrategyKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class RsiStrategyProtoTest {

    private static final my.signals.v1.Symbol BTCUSDT_PROTO =
            my.signals.v1.Symbol.newBuilder().setBase("BTC").setQuote("USDT").build();

    private RsiStrategy strategy;
    private InMemoryUnitPositionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryUnitPositionStore();
        strategy = new RsiStrategy(store); // unitValueQuote=5, profitThreshold=1%
    }

    @Test
    void buy_STRONG_returnsOneAggregatedInstruction_andOnExecutedSplitsInto4Units() {
        // При price=20000 и unitValueQuote=5:
        // perUnitBase = 5 / 20000 = 0.00025000; STRONG => 4 юнита; aggregated = 0.00100000
        Signal s = Signal.newBuilder()
                .setId("s1")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.STRONG)
                .setOperation(OperationType.BUY)
                .setPrice(20000.0)
                .build();

        var instrs = strategy.decide(s);
        assertEquals(1, instrs.size(), "BUY должен вернуть одну агрегированную инструкцию");
        var instr = instrs.get(0);

        assertEquals(OperationType.BUY, instr.operationType());
        bdAssertEq(bd("0.0600000"), instr.baseQty());

        // эмулируем fill на бирже
        strategy.onExecuted(instr, new OrderExecutionResult(
                UUID.randomUUID().toString(), bd("20000"), instr.baseQty()
        ));

        // В сторе — 4 юнита по 0.00025000, сумма 0.00100000
        assertEquals(4, store.usedUnits());
        bdAssertEq(bd("0.0600000"), store.snapshot().stream().map(u -> u.baseQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        store.snapshot().forEach(u -> bdAssertEq(bd("0.015000"), u.baseQty));
        store.snapshot().forEach(u -> bdAssertEq(bd("20000"), u.purchasePrice));
    }

    @Test
    void buy_withRemainder_lastUnitGetsRemainder() {
        // SMALL => 2 юнита
        Signal s = Signal.newBuilder()
                .setId("s2")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.SMALL)
                .setOperation(OperationType.BUY)
                .setPrice(20000.0)
                .build();

        var instr = strategy.decide(s).get(0);

        // Исполнено с остатком: 0.05000001 → per=0.02500000, last=0.02500001
        BigDecimal executed = bd("0.05000001");
        strategy.onExecuted(instr, new OrderExecutionResult(
                UUID.randomUUID().toString(), bd("20010"), executed
        ));

        assertEquals(2, store.usedUnits());
        var list = store.snapshot();
        BigDecimal per = executed.divide(bd("2"), 8, RoundingMode.DOWN);
        bdAssertEq(per, list.get(0).baseQty);
        bdAssertEq(executed.subtract(per), list.get(1).baseQty);
        list.forEach(u -> bdAssertEq(bd("20010"), u.purchasePrice));
    }

    @Test
    void sell_belowThreshold_returnsNoInstruction() {
        // Есть один юнит @19000
        store.add(new UnitPositionStore.UnitPosition(UUID.randomUUID(),
                Symbol.fromProto(BTCUSDT_PROTO), bd("19000"), bd("0.01000000"), Instant.now()
        ));

        // 19100 < 19190 → инструкций нет (нужен хотя бы 1% разницы)
        Signal s = Signal.newBuilder()
                .setId("s3")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE) // MIDDLE продаёт до 1 юнита
                .setOperation(OperationType.SELL)
                .setPrice(19100.0)
                .build();

        assertTrue(strategy.decide(s).isEmpty());
        assertEquals(1, store.usedUnits());
    }

    @Test
    void sell_aboveThreshold_returnsInstructionForCheapest_andOnExecutedRemovesIt() {
        var cheap = new UnitPositionStore.UnitPosition(UUID.randomUUID(),
                Symbol.fromProto(BTCUSDT_PROTO), bd("19000"), bd("0.01111111"), Instant.now());
        var exp   = new UnitPositionStore.UnitPosition(UUID.randomUUID(),
                Symbol.fromProto(BTCUSDT_PROTO), bd("20000"), bd("0.01000000"), Instant.now());
        store.add(exp);
        store.add(cheap);
        assertEquals(2, store.usedUnits());

        // MIDDLE => хотим продать до 1 позиции; при 19200 проходит только cheap (19190 порог)
        Signal s = Signal.newBuilder()
                .setId("s4")
                .setSymbol(BTCUSDT_PROTO)
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE) // ВАЖНО: SMALL теперь не продаёт (0)
                .setOperation(OperationType.SELL)
                .setPrice(19200.0)
                .build();

        var sellInstrs = strategy.decide(s);
        assertEquals(1, sellInstrs.size());
        var sell = sellInstrs.get(0);

        assertEquals(OperationType.SELL, sell.operationType());
        // Продаём самый дешёвый
        bdAssertEq(cheap.baseQty, sell.baseQty());
        assertEquals(cheap.id, sell.positionRef());

        // onExecuted(SELL) удаляет выбранный юнит
        strategy.onExecuted(sell, new OrderExecutionResult(
                UUID.randomUUID().toString(), bd("19200"), sell.baseQty()
        ));

        assertEquals(1, store.usedUnits());
        bdAssertEq(bd("20000"), store.snapshot().get(0).purchasePrice);
    }

    // ---- helpers ----
    private static BigDecimal bd(String s) { return new BigDecimal(s); }
    private static void bdAssertEq(BigDecimal exp, BigDecimal act) {
        assertEquals(0, exp.compareTo(act),
                "Expected " + exp.toPlainString() + " but was " + act.toPlainString());
    }
}
