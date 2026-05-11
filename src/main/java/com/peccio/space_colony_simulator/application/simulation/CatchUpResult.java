package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.ColonyEvent;

import java.util.List;

/**
 * Result of a catch-up run for one colony.
 * Contains events from ALL ticks processed (not just the last one).
 * This is the data source for the future replay feature.
 */

public record CatchUpResult(
        Long colonyId,
        String colonyName,
        int ticksProcessed,
        List<ColonyEvent> allEvents
) {
    /** True when the colony had to catch up more than one missed tick. */
    public boolean hadMissedTicks() {
        return ticksProcessed > 1;
    }
}
