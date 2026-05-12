package com.peccio.space_colony_simulator.application.replay;

import java.time.LocalDateTime;
/*
 * Lightweight event representation for API responses
 */
public record EventSummary(
        Long            id,
        String          eventType,
        String          severity,
        String          description,
        LocalDateTime simOccurredAt,
        LocalDateTime   simResolvedAt,
        boolean         resolved
) {}
