package com.peccio.space_colony_simulator.application.colony;

import com.peccio.space_colony_simulator.domain.model.Colony;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.UserEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ColonyManagementService {

    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;
    private final ColonyEventRepository eventRepository;
    private final AuthenticatedUserService authService;

    public ColonyManagementService(ColonyRepository colonyRepository, ColonyResourceRepository resourceRepository, ColonyEventRepository eventRepository, AuthenticatedUserService authService) {
        this.colonyRepository = colonyRepository;
        this.resourceRepository = resourceRepository;
        this.eventRepository = eventRepository;
        this.authService = authService;
    }

    // List my colonies
    @Transactional(readOnly = true)
    public List<ColonySummaryResponse> getMyColonies() {
        Long userId = authService.getCurrentUserId();
        return colonyRepository.findAllByUserId(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    //create a colony
    @Transactional
    public ColonySummaryResponse createColony(CreateColonyRequest request) {
        UserEntity user = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        ColonyEntity colony = ColonyEntity.builder()
                .name(request.name())
                .ownerId(user.getUsername())
                .user(user)
                .population(10)
                .status("ACTIVE")
                .foundedAt(now)
                .lastTickAt(now)
                .lastProcessedAt(now)
                .build();

        colony = colonyRepository.save(colony);

        // Default balanced starting resources
        resourceRepository.saveAll(List.of(
                resource(colony, "OXYGEN",    800, 10, 8,  1000),
                resource(colony, "FOOD",      600, 8,  6,  1000),
                resource(colony, "ENERGY",    500, 12, 10, 1000),
                resource(colony, "MATERIALS", 400, 5,  4,  1000)
        ));

        log.info("Colony '{}' created for user '{}'", colony.getName(), user.getUsername());
        return toSummary(colony);
    }

    private ColonySummaryResponse toSummary(ColonyEntity entity) {
        int eventCount = eventRepository
                .findAllByColonyIdAndResolved(entity.getId(), false).size();

        return new ColonySummaryResponse(
                entity.getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getPopulation(),
                entity.getLastTickAt().toString(),
                eventCount
        );
    }

    private ColonyResourceEntity resource(
            ColonyEntity colony, String type,
            double current, double prod, double cons, double cap) {

        return ColonyResourceEntity.builder()
                .colony(colony)
                .resourceType(type)
                .currentAmount(BigDecimal.valueOf(current))
                .productionRate(BigDecimal.valueOf(prod))
                .consumptionRate(BigDecimal.valueOf(cons))
                .storageCapacity(BigDecimal.valueOf(cap))
                .build();
    }
}
