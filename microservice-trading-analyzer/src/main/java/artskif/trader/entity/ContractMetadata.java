package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Метаданные для контракта (фичи и лейблы)
 * Содержит информацию о каждой фиче или лейбле, используемой в контракте
 */
@Entity
@Table(name = "contract_metadata")
public class ContractMetadata extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(name = "sequence_order", nullable = false, unique = true)
    public Integer sequenceOrder;

    @Column(name = "data_type", nullable = false, length = 50)
    public String dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_type", nullable = false, length = 20)
    public MetadataType metadataType;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    public Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    public Contract contract;

    public ContractMetadata() {
    }

    public ContractMetadata(String name, String description, Integer sequenceOrder, String dataType, MetadataType metadataType, Contract contract) {
        // id будет сгенерирован автоматически при persist
        this.name = name;
        this.description = description;
        this.sequenceOrder = sequenceOrder;
        this.dataType = dataType;
        this.metadataType = metadataType;
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








