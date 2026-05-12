package com.peccio.space_colony_simulator.application.replay;

import java.math.BigDecimal;

/**
 * Snapshot of one resource at the current simulation moment.
 * inShortage and criticallyLow are computed flags for the UI.
 */
public record ResourceStateDto(
        String     resourceType,
        BigDecimal currentAmount,
        BigDecimal productionRate,
        BigDecimal consumptionRate,
        BigDecimal storageCapacity,
        boolean    inShortage,
        boolean    criticallyLow
) {}
