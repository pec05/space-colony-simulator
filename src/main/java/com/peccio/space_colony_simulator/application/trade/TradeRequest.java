package com.peccio.space_colony_simulator.application.trade;

import com.peccio.space_colony_simulator.domain.model.ResourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
/**
 * Request to initiate a trade from one colony to another.
 */
public record TradeRequest(
        @NotNull(message = "Receiver colony ID is required")
        Long receiverColonyId,

        @NotNull(message = "Resource type is required")
        ResourceType resourceType,

        @NotNull @Positive(message = "Amount must be positive")
        BigDecimal amount
) {}
