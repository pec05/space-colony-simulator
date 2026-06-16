package com.peccio.space_colony_simulator.application.colony;
/**
 * Lightweight colony summary for the dashboard list.
 * Full state is loaded only when the user opens a specific colony.
 */
public record ColonySummaryResponse(
        Long   id,
        String name,
        String status,
        int    population,
        String lastTickAt,
        int    activeEventCount
) {}
