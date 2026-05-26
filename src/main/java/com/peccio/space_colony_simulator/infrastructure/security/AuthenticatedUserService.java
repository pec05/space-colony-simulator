package com.peccio.space_colony_simulator.infrastructure.security;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.UserEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Single source of truth for:
 *   1. Extracting the currently authenticated user
 *   2. Validating colony ownership
 *
 * All services that operate on colony-specific data call this first.
 */
@Slf4j
@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final ColonyRepository colonyRepository;

    public AuthenticatedUserService(UserRepository userRepository, ColonyRepository colonyRepository) {
        this.userRepository = userRepository;
        this.colonyRepository = colonyRepository;
    }

    // Current user extraction

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found: " + auth.getName()));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    // Ownership validation

    /**
     * Throws 404 if colony doesn't exist.
     * Throws 403 if authenticated user doesn't own it.
     */
    public void requireColonyOwnership(Long colonyId) {
        if (!colonyRepository.existsById(colonyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colony not found: " + colonyId);
        }

        Long userId = getCurrentUserId();

        if (!colonyRepository.existsByIdAndUserId(colonyId, userId)) {
            log.warn("Ownership violation: userId={} tried to access colonyId={}",
                    userId, colonyId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this colony");
        }
    }

}
