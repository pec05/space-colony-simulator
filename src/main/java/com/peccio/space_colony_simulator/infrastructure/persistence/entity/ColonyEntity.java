package com.peccio.space_colony_simulator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "colonies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColonyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false)
    private int population;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "founded_at", nullable = false)
    private LocalDateTime foundedAt;

    @Column(name = "last_tick_at", nullable = false)
    private LocalDateTime lastTickAt;

    @Column(name = "last_processed_at", nullable = false)
    private LocalDateTime lastProcessedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "colony",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ColonyResourceEntity> resources = new ArrayList<>();
}
