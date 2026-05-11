package com.peccio.space_colony_simulator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for simulation.tick.* properties in application.yml.
 * Avoids scattered @Value annotations throughout the codebase.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "simulation.tick")
public class SimulationProperties {

    /** How often the scheduler fires, in milliseconds. */
    private long rateMs = 60000;

    /** How many simulation hours advance per tick. */
    private int simHoursPerTick = 1;
}
