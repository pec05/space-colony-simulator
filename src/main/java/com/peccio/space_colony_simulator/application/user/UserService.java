package com.peccio.space_colony_simulator.application.user;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.UserEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ColonyRepository colonyRepository;

    public UserService(UserRepository userRepository, ColonyRepository colonyRepository) {
        this.userRepository = userRepository;
        this.colonyRepository = colonyRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username '%s' is already taken".formatted(request.username()));
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email '%s' is already registered".formatted(request.email()));
        }

        UserEntity entity = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .build();

        UserEntity saved = userRepository.save(entity);

        log.info("User created — id={} username={}", saved.getId(), saved.getUsername());

        return toResponse(saved, 0);
    }

    // Queries
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));

        int colonyCount = colonyRepository.countByUserId(id);
        return toResponse(entity, colonyCount);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(e -> toResponse(e, colonyRepository.countByUserId(e.getId())))
                .toList();
    }

    private UserResponse toResponse(UserEntity entity, int colonyCount) {
        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                colonyCount
        );
    }
}
