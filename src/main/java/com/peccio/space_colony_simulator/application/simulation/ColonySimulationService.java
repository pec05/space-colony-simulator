package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates simulation ticks across ALL active colonies.
 *
 * Intentionally NOT @Transactional at this level.
 * Each colony is ticked in its own transaction via ColonyTickService.
 * This guarantees fault isolation — one broken colony never blocks the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColonySimulationService {
    private final ColonyRepository colonyRepository;
    private final CatchUpService catchUpService;

    /**
     * Entry point called by the scheduler every tick interval.
     * Loads all ACTIVE colonies and processes each one independently.
     */
    public void runTick() {
        List<ColonyEntity> activeColonies = colonyRepository.findAllByStatus("ACTIVE");

        log.info("=== Simulation tick started — {} active colony(ies) ===",
                activeColonies.size());

        int success = 0;
        int failure = 0;

        for (ColonyEntity colony : activeColonies) {
            try {
                CatchUpResult result = catchUpService.catchUp(colony.getId());
                if (result.hadMissedTicks()) {
                    log.info("↩ Colony '{}' caught up {} missed tick(s)",
                            result.colonyName(), result.ticksProcessed());
                }
                success++;
            } catch (Exception ex) {
                failure++;
                log.error("Tick failed for colony id={} name='{}' — skipping",
                        colony.getId(), colony.getName(), ex);
            }
        }

        log.info("=== Simulation tick complete — success={} failure={} ===",
                success, failure);
    }
}
