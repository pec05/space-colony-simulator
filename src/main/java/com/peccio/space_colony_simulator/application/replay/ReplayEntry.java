package com.peccio.space_colony_simulator.application.replay;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents one simulation tick in the replay timeline.
 * tickNumber is sequential (1, 2, 3...) starting from the replay's start point.
 * simTime is the simulation timestamp of that tick.
 * events is empty when the tick passed without incident.
 */
public record ReplayEntry(
        int             tickNumber,
        LocalDateTime simTime,
        List<EventSummary> events
) {}
