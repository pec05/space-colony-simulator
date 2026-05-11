package com.peccio.space_colony_simulator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "colony_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColonyResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colony_id", nullable = false)
    private ColonyEntity colony;

    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    @Column(name = "current_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAmount;

    @Column(name = "production_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal productionRate;

    @Column(name = "consumption_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumptionRate;

    @Column(name = "storage_capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal storageCapacity;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
