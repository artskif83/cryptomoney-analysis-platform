package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Метаданные для фич (параметров ML)
 * Содержит информацию о каждой фиче, используемой в контракте
 */
@Entity
@Table(name = "contract_features_metadata")
public class ContractFeatureMetadata extends PanacheEntityBase {

    @Id
    @Column(name = "feature_name", nullable = false, length = 255)
    public String featureName;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(name = "sequence_order", nullable = false, unique = true)
    public Integer sequenceOrder;

    @Column(name = "data_type", nullable = false, length = 50)
    public String dataType;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    public Contract contract;

    public ContractFeatureMetadata() {
    }

    public ContractFeatureMetadata(String featureName, String description, Integer sequenceOrder, String dataType, Contract contract) {
        this.featureName = featureName;
        this.description = description;
        this.sequenceOrder = sequenceOrder;
        this.dataType = dataType;
        this.contract = contract;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}








