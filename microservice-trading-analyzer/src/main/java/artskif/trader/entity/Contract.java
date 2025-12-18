package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Контракт - набор фич (параметров ML)
 * Один контракт содержит много метаданных фич
 */
@Entity
@Table(name = "contracts")
public class Contract extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(name = "feature_set_id", nullable = false, length = 50)
    public String featureSetId;

    @Column(name = "contract_hash", length = 64)
    public String contractHash;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant updatedAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ContractMetadata> metadata = new ArrayList<>();

    public Contract() {
    }

    public Contract(String name, String description, String featureSetId) {
        this.name = name;
        this.description = description;
        this.featureSetId = featureSetId;
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

    /**
     * Добавить метаданные к контракту
     */
    public void addMetadata(ContractMetadata feature) {
        metadata.add(feature);
        feature.contract = this;
    }

    /**
     * Удалить метаданные из контракта
     */
    public void removeMetadata(ContractMetadata feature) {
        metadata.remove(feature);
        feature.contract = null;
    }
}

