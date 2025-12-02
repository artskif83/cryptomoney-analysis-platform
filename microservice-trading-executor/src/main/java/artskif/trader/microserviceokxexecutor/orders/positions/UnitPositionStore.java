package artskif.trader.microserviceokxexecutor.orders.positions;

import artskif.trader.microserviceokxexecutor.orders.strategy.list.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitPositionStore {

    final class UnitPosition {
        public final UUID id;
        public final Symbol symbol;
        public final BigDecimal purchasePrice; // в котируемой валюте
        public final BigDecimal baseQty;       // приобретённое количество базовой монеты
        public final Instant purchasedAt;

        public UnitPosition(UUID id, Symbol symbol, BigDecimal purchasePrice, BigDecimal baseQty, Instant purchasedAt) {
            this.id = id;
            this.symbol = symbol;
            this.purchasePrice = purchasePrice;
            this.baseQty = baseQty;
            this.purchasedAt = purchasedAt;
        }
    }

    void add(UnitPosition unit);
    Optional<UnitPosition> peekLowest();
    List<UnitPosition> peekLowestN(int n);
    Optional<UnitPosition> pollLowest();
    Optional<UnitPosition> findById(UUID id);
    boolean removeById(UUID id);
    int usedUnits();
    List<UnitPosition> snapshot();
    void clear();
}
