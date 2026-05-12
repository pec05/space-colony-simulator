package com.peccio.space_colony_simulator.application.replay;

import java.time.LocalDateTime;
import java.util.List;
/**
 * Full current-state snapshot of a colony.
 * Used by the "just show me the result" path.
 */
public record ColonyStateResponse(
        Long                  id,
        String                name,
        String                status,
        int                   population,
        LocalDateTime         lastTickAt,
        LocalDateTime lastProcessedAt,
        List<ResourceStateDto>  resources,
        List<EventSummary> activeEvents
) {}
