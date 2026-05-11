package com.peccio.space_colony_simulator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * An event that occurred in a colony during simulation.
 * sim_occurred_at and sim_resolved_at track SIMULATION time,
 * not wall-clock time — this distinction is critical for the replay system.
 */

@Getter
@Builder
public class ColonyEvent {

    private final Long id;
    private final Long colonyId;
    private final EventType eventType;
    private final EventSeverity severity;
    private final String description;
    private final LocalDateTime simOccurredAt;

    private LocalDateTime simResolvedAt;
    private boolean resolved;

    public void resolve(LocalDateTime simTime) {
        this.resolved = true;
        this.simResolvedAt = simTime;
    }
}
