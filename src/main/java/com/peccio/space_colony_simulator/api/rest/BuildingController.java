package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.building.BuildingRequest;
import com.peccio.space_colony_simulator.application.building.BuildingResponse;
import com.peccio.space_colony_simulator.application.building.BuildingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    /**
     * List all available building types with costs and bonuses.
     * GET /api/buildings/catalog
     */
    @GetMapping("/buildings/catalog")
    public ResponseEntity<List<BuildingResponse>> getCatalog() {
        return ResponseEntity.ok(buildingService.getCatalog());
    }

    /**
     * Construct a building in a colony.
     * POST /api/colonies/{colonyId}/buildings
     */
    @PostMapping("/colonies/{colonyId}/buildings")
    public ResponseEntity<BuildingResponse> construct(
            @PathVariable Long colonyId,
            @RequestBody @Valid BuildingRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(buildingService.construct(colonyId, request));
    }

    /**
     * List all buildings in a colony.
     * GET /api/colonies/{colonyId}/buildings
     */
    @GetMapping("/colonies/{colonyId}/buildings")
    public ResponseEntity<List<BuildingResponse>> getBuildings(
            @PathVariable Long colonyId) {

        return ResponseEntity.ok(buildingService.getBuildingsForColony(colonyId));
    }
}
