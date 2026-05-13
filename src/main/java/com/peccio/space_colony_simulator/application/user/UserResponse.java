package com.peccio.space_colony_simulator.application.user;

import java.time.LocalDateTime;

/**
 * API response for user data.
 */
public record UserResponse(
        Long          id,
        String        username,
        String        email,
        LocalDateTime createdAt,
        int           colonyCount
) {}
