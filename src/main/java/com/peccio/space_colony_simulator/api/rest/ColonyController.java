package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.colony.ColonyManagementService;
import com.peccio.space_colony_simulator.application.colony.ColonySummaryResponse;
import com.peccio.space_colony_simulator.application.colony.CreateColonyRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/colonies")
public class ColonyController {

    private final ColonyManagementService colonyManagementService;

    public ColonyController(ColonyManagementService colonyManagementService) {
        this.colonyManagementService = colonyManagementService;
    }

    /**
     * GET /api/colonies — returns all colonies owned by the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ColonySummaryResponse>> getMyColonies() {
        return ResponseEntity.ok(colonyManagementService.getMyColonies());
    }

    /**
     * POST /api/colonies — creates a new colony for the authenticated user
     */
    @PostMapping
    public ResponseEntity<ColonySummaryResponse> createColony(
            @RequestBody @Valid CreateColonyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(colonyManagementService.createColony(request));
    }
}
