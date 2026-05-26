package com.peccio.space_colony_simulator.application.auth;

public record AuthResponse(
        String token,
        Long   userId,
        String username,
        String email
) {}
