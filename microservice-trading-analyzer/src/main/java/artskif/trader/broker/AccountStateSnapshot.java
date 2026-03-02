package artskif.trader.broker;

import artskif.trader.entity.PendingOrder;
import artskif.trader.entity.Position;

import java.time.Instant;
import java.util.List;

/**
 * Снимок состояния аккаунта в определенный момент времени
 */
public class AccountStateSnapshot {

    private final Instant timestamp;
    private final List<PendingOrder> pendingOrders;
    private final List<Position> positions;
    /**
     * История позиций за последние 24 часа
     */
    private final List<Position> positionsHistory;

    public AccountStateSnapshot(
            Instant timestamp,
            List<PendingOrder> pendingOrders,
            List<Position> positions,
            List<Position> positionsHistory
    ) {
        this.timestamp = timestamp;
        this.pendingOrders = pendingOrders;
        this.positions = positions;
        this.positionsHistory = positionsHistory;
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

    public List<Position> getPositionsHistory() {
        return positionsHistory;
    }

    @Override
    public String toString() {
        return "AccountStateSnapshot{" +
                "timestamp=" + timestamp +
                ", pendingOrdersCount=" + (pendingOrders != null ? pendingOrders.size() : 0) +
                ", positionsCount=" + (positions != null ? positions.size() : 0) +
                ", positionsHistoryCount=" + (positionsHistory != null ? positionsHistory.size() : 0) +
                '}';
    }
}
