package com.peccio.space_colony_simulator.application.replay;

import java.util.List;

/**
 * Full replay payload — all ticks between a start point and now,
 * each with its events. Empty ticks are included so the client
 * can animate a smooth timeline.
 */
public record ReplayResponse(
        Long              colonyId,
        String            colonyName,
        int               totalTicks,
        int               totalEvents,
        List<ReplayEntry> ticks
) {}
