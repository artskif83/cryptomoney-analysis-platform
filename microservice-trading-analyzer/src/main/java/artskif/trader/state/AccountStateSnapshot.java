package artskif.trader.state;

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
    private final List<PendingOrder> pendingOrders;
    private final List<Position> positions;
    /** Текущий размер депозита с учётом всех открытых позиций (unrealized PnL) в USDT */
    private final BigDecimal totalEquityInUsdt;

    public AccountStateSnapshot(
            Instant timestamp,
            List<PendingOrder> pendingOrders,
            List<Position> positions,
            BigDecimal totalEquityInUsdt
    ) {
        this.timestamp = timestamp;
        this.pendingOrders = pendingOrders;
        this.positions = positions;
        this.totalEquityInUsdt = totalEquityInUsdt;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<PendingOrder> getPendingOrders() {
        return pendingOrders;
    }

    public List<Position> getPositions() {
        return positions;
    }

    /**
     * Текущий размер депозита с учётом всех открытых позиций (unrealized PnL) в USDT.
     * @return суммарный equity счёта в USDT или {@code null}, если данные недоступны
     */
    public BigDecimal getTotalEquityInUsdt() {
        return totalEquityInUsdt;
    }

    @Override
    public String toString() {
        return "AccountStateSnapshot{" +
                "timestamp=" + timestamp +
                ", pendingOrdersCount=" + (pendingOrders != null ? pendingOrders.size() : 0) +
                ", positionsCount=" + (positions != null ? positions.size() : 0) +
                ", totalEquityInUsdt=" + totalEquityInUsdt +
                '}';
    }
}
