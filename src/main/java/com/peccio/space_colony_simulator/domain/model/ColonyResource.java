package com.peccio.space_colony_simulator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Represents a single resource slot in a colony.
 * Mutation happens only through domain methods — never via setters.
 *
 * applyTick() is the core simulation method:
 * new_amount = clamp(current + production - consumption, 0, capacity)
 */

@Getter
@Builder
public class ColonyResource {

    private final Long id;
    private final Long colonyId;
    private final ResourceType resourceType;

    private BigDecimal currentAmount;
    private BigDecimal productionRate;
    private BigDecimal consumptionRate;
    private BigDecimal storageCapacity;

    /**
     * Net change per tick. Negative = consuming more than producing.
     */
    public BigDecimal getNetRate() {
        return productionRate.subtract(consumptionRate);
    }

    /**
     * Below 10% of capacity — warning threshold.
     */
    public boolean isCriticallyLow() {
        BigDecimal threshold = storageCapacity.multiply(BigDecimal.valueOf(0.10));
        return currentAmount.compareTo(threshold) < 0;
    }

    /**
     * Advances this resource by one simulation tick.
     * Clamps result between 0 and storageCapacity.
     */
    public void applyTick() {
        BigDecimal newAmount = currentAmount.add(getNetRate());
        this.currentAmount = newAmount
                .max(BigDecimal.ZERO)
                .min(storageCapacity);
    }
}
