package com.peccio.space_colony_simulator.application.building;

import com.peccio.space_colony_simulator.domain.model.BuildingType;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(

        @NotNull(message = "Building type is required")
        BuildingType type
) {}
