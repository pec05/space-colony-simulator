package com.peccio.space_colony_simulator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Pure domain representation of a user.
 * Authentication fields (password, roles) will be added in Phase 3.
 */
@Getter
@Builder
public class User {
    private final Long          id;
    private final String        username;
    private final String        email;
    private final LocalDateTime createdAt;
}
