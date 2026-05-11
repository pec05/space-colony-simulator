package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.simulation.ColonySimulationService;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Debug-only endpoints — NOT part of the production API.
 * Used during Phase 1 development to trigger ticks and inspect colony state.
 * Will be removed or secured in Phase 3.
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final ColonySimulationService simulationService;
    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;

    public DebugController(
            ColonySimulationService simulationService,
            ColonyRepository colonyRepository,
            ColonyResourceRepository resourceRepository) {

        this.simulationService  = simulationService;
        this.colonyRepository   = colonyRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Manually fires one simulation tick across all active colonies.
     * POST /api/debug/tick
     */
    @PostMapping("/tick")
    public ResponseEntity<String> manualTick() {
        log.info("Manual tick triggered via debug endpoint");
        simulationService.runTick();
        return ResponseEntity.ok("Tick executed successfully");
    }

    /**
     * Creates a test colony with all 4 resources pre-populated.
     * POST /api/debug/colony?name=Alpha
     */
    @PostMapping("/colony")
    public ResponseEntity<String> createTestColony(
            @RequestParam(defaultValue = "Alpha") String name) {

        LocalDateTime now = LocalDateTime.now();

        ColonyEntity colony = ColonyEntity.builder()
                .name(name)
                .ownerId("debug-user")
                .population(10)
                .status("ACTIVE")
                .foundedAt(now)
                .lastTickAt(now)
                .build();

        colony = colonyRepository.save(colony);
        final Long colonyId = colony.getId();

        List<ColonyResourceEntity> resources = buildStartingResources(colony);
        resourceRepository.saveAll(resources);

        log.info("Test colony '{}' created with id={}", name, colonyId);
        return ResponseEntity.ok(
                "Colony '%s' created with id=%d".formatted(name, colonyId)
        );
    }

    /**
     * Returns current state of all colonies and their resources.
     * GET /api/debug/colonies
     */
    @GetMapping("/colonies")
    public ResponseEntity<List<Map<String, Object>>> listColonies() {
        List<ColonyEntity> colonies = colonyRepository.findAll();

        List<Map<String, Object>> result = colonies.stream().map(colony -> {
            List<Map<String, Object>> resources = resourceRepository
                    .findAllByColonyId(colony.getId())
                    .stream()
                    .map(r -> Map.<String, Object>of(
                            "type",        r.getResourceType(),
                            "current",     r.getCurrentAmount(),
                            "production",  r.getProductionRate(),
                            "consumption", r.getConsumptionRate(),
                            "capacity",    r.getStorageCapacity()
                    ))
                    .toList();

            return Map.<String, Object>of(
                    "id",          colony.getId(),
                    "name",        colony.getName(),
                    "status",      colony.getStatus(),
                    "population",  colony.getPopulation(),
                    "lastTickAt",  colony.getLastTickAt().toString(),
                    "resources",   resources
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    private List<ColonyResourceEntity> buildStartingResources(ColonyEntity colony) {
        // Balanced starting state:
        // Production slightly exceeds consumption so the colony is stable at first.
        // Tweak rates to simulate shortages during testing.
        return List.of(
                resource(colony, "OXYGEN",    800, 10, 8,  1000),
                resource(colony, "FOOD",      600, 8,  6,  1000),
                resource(colony, "ENERGY",    500, 12, 10, 1000),
                resource(colony, "MATERIALS", 400, 5,  4,  1000)
        );
    }

    private ColonyResourceEntity resource(
            ColonyEntity colony,
            String type,
            double current,
            double production,
            double consumption,
            double capacity) {

        return ColonyResourceEntity.builder()
                .colony(colony)
                .resourceType(type)
                .currentAmount(BigDecimal.valueOf(current))
                .productionRate(BigDecimal.valueOf(production))
                .consumptionRate(BigDecimal.valueOf(consumption))
                .storageCapacity(BigDecimal.valueOf(capacity))
                .build();
    }
}
