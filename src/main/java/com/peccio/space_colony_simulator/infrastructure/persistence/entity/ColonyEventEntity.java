package com.peccio.space_colony_simulator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "colony_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColonyEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colony_id", nullable = false)
    private ColonyEntity colony;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sim_occurred_at", nullable = false)
    private LocalDateTime simOccurredAt;

    @Column(name = "sim_resolved_at")
    private LocalDateTime simResolvedAt;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
