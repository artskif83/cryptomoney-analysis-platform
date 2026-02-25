package artskif.trader.broker;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Снимок состояния аккаунта в определенный момент времени
 */
public class AccountStateSnapshot {

    private final Instant timestamp;
    private final BigDecimal usdtBalance;
    private final List<Map<String, Object>> pendingOrders;
    private final List<Map<String, Object>> positions;

    public AccountStateSnapshot(
            Instant timestamp,
            BigDecimal usdtBalance,
            List<Map<String, Object>> pendingOrders,
            List<Map<String, Object>> positions
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

    public List<Map<String, Object>> getPendingOrders() {
        return pendingOrders;
    }

    public List<Map<String, Object>> getPositions() {
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
