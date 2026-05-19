package com.peccio.space_colony_simulator.application.trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
        Long          id,
        Long          senderColonyId,
        String        senderColonyName,
        Long          receiverColonyId,
        String        receiverColonyName,
        String        resourceType,
        BigDecimal amount,
        String        status,
        LocalDateTime initiatedSimTime,
        LocalDateTime etaSimTime,
        LocalDateTime arrivedAt
) {}
