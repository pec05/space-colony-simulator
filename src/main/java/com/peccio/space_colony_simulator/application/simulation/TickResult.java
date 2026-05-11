package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.ColonyEvent;

import java.util.List;

/**
 * Immutable snapshot of what happened during a single simulation tick
 * for one colony. Produced by TickProcessor, consumed by ColonyTickService.
 */
public record TickResult(
        Long colonyId,
        List<ColonyEvent> generatedEvents
) {

    public boolean hasEvent() {
        return !generatedEvents.isEmpty();
    }
}
