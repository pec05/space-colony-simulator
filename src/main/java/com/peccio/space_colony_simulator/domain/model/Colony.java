package com.peccio.space_colony_simulator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
public class Colony {

    private final Long id;
    private final String name;
    private final String ownerId;
    private final LocalDateTime foundedAt;

    private int population;
    private ColonyStatus status;
    private LocalDateTime lastTickAt;

    @Builder.Default
    private List<ColonyResource> resources = new ArrayList<>();

    public boolean isActive() {
        return ColonyStatus.ACTIVE.equals(status);
    }

    /**
     * Called by the simulation engine after each tick is processed.
     */
    public void advanceLastTickAt(LocalDateTime newTickTime) {
        this.lastTickAt = newTickTime;
    }

    /**
     * Population changes drive colony survival.
     * If population hits 0, the colony is destroyed.
     */
    public void applyPopulationChange(int delta) {
        this.population = Math.max(0, this.population + delta);
        if (this.population == 0) {
            this.status = ColonyStatus.DESTROYED;
        }
    }

    public void abandon() {
        this.status = ColonyStatus.ABANDONED;
    }

    /**
     * Convenience: find a specific resource without exposing the full list.
     */
    public Optional<ColonyResource> getResource(ResourceType type) {
        return resources.stream()
                .filter(r -> r.getResourceType() == type)
                .findFirst();
    }
}
