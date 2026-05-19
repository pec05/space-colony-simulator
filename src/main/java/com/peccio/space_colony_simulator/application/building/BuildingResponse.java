package com.peccio.space_colony_simulator.application.building;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BuildingResponse(
        Long          id,
        String        type,
        String        affectedResource,
        BigDecimal productionBonus,
        BigDecimal    constructionCost,
        LocalDateTime builtAt
) {}
