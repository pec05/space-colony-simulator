package com.peccio.space_colony_simulator.infrastructure.scheduler;

import com.peccio.space_colony_simulator.application.simulation.ColonySimulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The clock of the simulation world.
 *
 * Fires ColonySimulationService.runTick() at a fixed interval
 * defined in application.yml (simulation.tick.rate-ms).
 *
 * This class has exactly one responsibility: triggering the tick.
 * All logic lives in the service layer.
 */
@Slf4j
@Component
public class SimulationScheduler {

    private final ColonySimulationService simulationService;

    public SimulationScheduler(ColonySimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Scheduled(fixedDelayString = "${simulation.tick.rate-ms:60000}")
    public void scheduledTick() {
        log.info("Scheduled tick triggered");
        simulationService.runTick();
    }
}
