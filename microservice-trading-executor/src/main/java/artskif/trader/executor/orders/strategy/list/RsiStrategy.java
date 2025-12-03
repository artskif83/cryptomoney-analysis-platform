package artskif.trader.executor.orders.strategy.list;

import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.positions.OrderInstruction;
import artskif.trader.executor.orders.positions.UnitPositionStore;
import artskif.trader.executor.orders.strategy.Strategy;
import my.signals.v1.OperationType;
import my.signals.v1.Signal;
import my.signals.v1.SignalLevel;
import my.signals.v1.StrategyKind;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BUY:
 *  - по уровню (STRONG/MIDDLE/SMALL) определяем кол-во юнитов N
 *  - считаем размер одного юнита в базовой монете: unitValueQuote / price
 *  - ГЕНЕРИРУЕМ ОДНУ инструкцию на сумму N * perUnitBase (агрегированно)
 *  - после исполнения split'им фактический executedBaseQty на N и сохраняем N юнитов в стор
 *
 * SELL:
 *  - если текущая цена >= (minPurchasePrice * (1 + profitThreshold)), генерируем 1 инструкцию
 *    на продажу ровно baseQty самого дешёвого юнита; в positionRef кладём его id.
 */

@Component
public final class RsiStrategy implements Strategy {

    private final UnitPositionStore store;
    private final BigDecimal unitValueQuote;      // стоимость 1 "юнита" в котируемой валюте (например, 300 USDT)
    private final BigDecimal profitThreshold;     // 0.01 = +1%

    private final Map<SignalLevel, Integer> buyUnits = new EnumMap<>(SignalLevel.class);
    private final Map<SignalLevel, Integer> sellUnits = new EnumMap<>(SignalLevel.class);

    // Планы незавершённых покупок: instructionId -> N (сколько юнитов нужно разложить после fill)
    private final Map<String, Integer> pendingBuyUnits = new ConcurrentHashMap<>();

    public RsiStrategy(UnitPositionStore store) {
        this.store = store;
        this.unitValueQuote = BigDecimal.valueOf(10);
        this.profitThreshold = BigDecimal.valueOf(0.01);

        // BUY
        buyUnits.put(SignalLevel.STRONG, 4);
        buyUnits.put(SignalLevel.MIDDLE, 3);
        buyUnits.put(SignalLevel.SMALL,  2);

        // SELL
        sellUnits.put(SignalLevel.STRONG, 2);
        sellUnits.put(SignalLevel.MIDDLE, 1);
        sellUnits.put(SignalLevel.SMALL,  0); // можно поменять на 1, если нужно
    }

    @Override
    public boolean supports(StrategyKind kind) {
        return kind == StrategyKind.RSI_DUAL_TF;
    }

    @Override
    public List<OrderInstruction> decide(Signal signal) {
        if (signal.getOperation() == OperationType.BUY) {
            int units = buyUnits.getOrDefault(signal.getLevel(), 1);
            // размер одного юнита в базовой монете (например BTC), 8 знаков после запятой
            BigDecimal perUnitBase = unitValueQuote.divide(BigDecimal.valueOf(signal.getPrice()), 8, RoundingMode.DOWN);
            // агрегированный объём на покупку одним ордером
            BigDecimal totalBase = perUnitBase.multiply(BigDecimal.valueOf(units))
                    .setScale(8, RoundingMode.DOWN);

            OrderInstruction instr = OrderInstruction.buy(Symbol.fromProto(signal.getSymbol()), totalBase);
            pendingBuyUnits.put(instr.instructionId(), units);
            return List.of(instr);
        } else { // SELL
            int toSell = sellUnits.getOrDefault(signal.getLevel(), 1);
            if (toSell <= 0) return List.of();

            // Берём до N самых дешёвых позиций без удаления
            var candidates = store.peekLowestN(toSell);
            if (candidates.isEmpty()) return List.of();

            BigDecimal mkt = BigDecimal.valueOf(signal.getPrice());
            BigDecimal onePlus = BigDecimal.ONE.add(profitThreshold);

            // Для каждой позиции проверяем, что текущая цена >= (purchasePrice * (1 + threshold))
            return candidates.stream()
                    .filter(u -> mkt.compareTo(u.purchasePrice.multiply(onePlus)) >= 0)
                    .map(u -> OrderInstruction.sell(Symbol.fromProto(signal.getSymbol()), u.baseQty, u.id))
                    .toList();
        }
    }

    @Override
    public void onExecuted(OrderInstruction instruction, OrderExecutionResult fill) {
        if (instruction.operationType() == OperationType.BUY) {
            // Разложить фактически исполненный объём по N юнитам
            int units = pendingBuyUnits.getOrDefault(instruction.instructionId(), 1);
            pendingBuyUnits.remove(instruction.instructionId());

            BigDecimal executed = fill.executedBaseQty() != null ? fill.executedBaseQty()
                    : instruction.baseQty();
            BigDecimal price = fill.avgPrice() != null ? fill.avgPrice() : BigDecimal.ZERO;

            // Равномерно на (units-1) юнитов с округлением вниз, последний — остаток, чтобы сумма сошлась
            BigDecimal per = executed.divide(BigDecimal.valueOf(units), 8, RoundingMode.DOWN);
            BigDecimal acc = BigDecimal.ZERO;

            for (int i = 0; i < units - 1; i++) {
                store.add(new UnitPositionStore.UnitPosition(UUID.randomUUID(),
                        instruction.symbol(), price, per, Instant.now()
                ));
                acc = acc.add(per);
            }
            BigDecimal last = executed.subtract(acc).setScale(8, RoundingMode.DOWN);
            if (last.compareTo(BigDecimal.ZERO) > 0) {
                store.add(new UnitPositionStore.UnitPosition(UUID.randomUUID(),
                        instruction.symbol(), price, last, Instant.now()
                ));
            }
        } else {
            // SELL: закрываем выбранный юнит по его ID
            if (instruction.positionRef() != null) {
                store.removeById(instruction.positionRef());
            }
        }
    }
}
