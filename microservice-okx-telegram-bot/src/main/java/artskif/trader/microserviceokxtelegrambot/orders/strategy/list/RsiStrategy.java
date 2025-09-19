package artskif.trader.microserviceokxtelegrambot.orders.strategy.list;

import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderInstruction;
import artskif.trader.microserviceokxtelegrambot.orders.positions.UnitPositionStore;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Level;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Side;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Signal2;
import artskif.trader.microserviceokxtelegrambot.orders.strategy.Strategy;
import my.signals.v1.Signal;
import my.signals.v1.StrategyKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
public final class RsiStrategy implements Strategy {

    private final UnitPositionStore store;
    private final BigDecimal unitValueQuote;      // стоимость 1 "юнита" в котируемой валюте (например, 300 USDT)
    private final BigDecimal profitThreshold;     // 0.01 = +1%

    private final Map<Level, Integer> buyUnits = new EnumMap<>(Level.class);

    // Планы незавершённых покупок: instructionId -> N (сколько юнитов нужно разложить после fill)
    private final Map<String, Integer> pendingBuyUnits = new ConcurrentHashMap<>();

    public RsiStrategy(UnitPositionStore store,
                       BigDecimal unitValueQuote,
                       BigDecimal profitThreshold) {
        this.store = store;
        this.unitValueQuote = unitValueQuote;
        this.profitThreshold = profitThreshold;

        buyUnits.put(Level.STRONG, 4);
        buyUnits.put(Level.MIDDLE, 3);
        buyUnits.put(Level.SMALL,  2);
    }

    @Override
    public boolean supports(StrategyKind kind) {
        return kind == StrategyKind.ADX_RSI;
    }

    @Override
    public List<OrderInstruction> decide(Signal2 signal) {
        if (signal.side() == Side.BUY) {
            int units = buyUnits.getOrDefault(signal.level(), 1);
            // размер одного юнита в базовой монете (например BTC), 8 знаков после запятой
            BigDecimal perUnitBase = unitValueQuote.divide(signal.price(), 8, RoundingMode.DOWN);
            // агрегированный объём на покупку одним ордером
            BigDecimal totalBase = perUnitBase.multiply(BigDecimal.valueOf(units))
                    .setScale(8, RoundingMode.DOWN);

            OrderInstruction instr = OrderInstruction.buy(signal.symbol(), totalBase);
            pendingBuyUnits.put(instr.instructionId(), units);
            return List.of(instr);
        } else { // SELL
            var cheapestOpt = store.peekLowest();
            if (cheapestOpt.isEmpty()) return List.of();

            var cheapest = cheapestOpt.get();
            BigDecimal thresholdPrice = cheapest.purchasePrice.multiply(BigDecimal.ONE.add(profitThreshold));
            if (signal.price().compareTo(thresholdPrice) >= 0) {
                // продаём ровно объём этого юнита
                return List.of(OrderInstruction.sell(signal.symbol(), cheapest.baseQty, cheapest.id));
            }
            return List.of();
        }
    }

    @Override
    public void onExecuted(OrderInstruction instruction, OrderExecutionResult fill) {
        if (instruction.side() == Side.BUY) {
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
                store.add(new UnitPositionStore.UnitPosition(
                        instruction.symbol(), price, per, Instant.now()
                ));
                acc = acc.add(per);
            }
            BigDecimal last = executed.subtract(acc).setScale(8, RoundingMode.DOWN);
            if (last.compareTo(BigDecimal.ZERO) > 0) {
                store.add(new UnitPositionStore.UnitPosition(
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
