package artskif.trader.broker;

import artskif.trader.entity.PendingOrder;
import artskif.trader.entity.Position;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Снимок состояния аккаунта в определенный момент времени
 */
public class AccountStateSnapshot {

    private final Instant timestamp;
    private final BigDecimal usdtBalance;
    private final List<PendingOrder> pendingOrders;
    private final List<Position> positions;

    public AccountStateSnapshot(
            Instant timestamp,
            BigDecimal usdtBalance,
            List<PendingOrder> pendingOrders,
            List<Position> positions
    ) {
        this.timestamp = timestamp;
        this.usdtBalance = usdtBalance;
        this.pendingOrders = pendingOrders;
        this.positions = positions;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public BigDecimal getUsdtBalance() {
        return usdtBalance;
    }

    public List<PendingOrder> getPendingOrders() {
        return pendingOrders;
    }

    public List<Position> getPositions() {
        return positions;
    }

    @Override
    public String toString() {
        return "AccountStateSnapshot{" +
                "timestamp=" + timestamp +
                ", usdtBalance=" + usdtBalance +
                ", pendingOrdersCount=" + (pendingOrders != null ? pendingOrders.size() : 0) +
                ", positionsCount=" + (positions != null ? positions.size() : 0) +
                '}';
    }
}
