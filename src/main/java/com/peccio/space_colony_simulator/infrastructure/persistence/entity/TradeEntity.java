package com.peccio.space_colony_simulator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_colony_id", nullable = false)
    private ColonyEntity senderColony;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_colony_id", nullable = false)
    private ColonyEntity receiverColony;

    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "initiated_sim_time", nullable = false)
    private LocalDateTime initiatedSimTime;

    @Column(name = "eta_sim_time", nullable = false)
    private LocalDateTime etaSimTime;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
