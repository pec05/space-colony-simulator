package com.peccio.space_colony_simulator.application.building;

import com.peccio.space_colony_simulator.domain.model.BuildingType;
import com.peccio.space_colony_simulator.domain.model.ResourceType;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.BuildingEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.BuildingRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;
    private final AuthenticatedUserService authService;

    public BuildingService(BuildingRepository buildingRepository, ColonyRepository colonyRepository, ColonyResourceRepository resourceRepository, AuthenticatedUserService authService) {
        this.buildingRepository = buildingRepository;
        this.colonyRepository = colonyRepository;
        this.resourceRepository = resourceRepository;
        this.authService = authService;
    }

    /**
     * Constructs a building in the given colony.
     *
     * Steps:
     *  1. Validate colony is active
     *  2. Validate colony has enough MATERIALS
     *  3. Deduct MATERIALS construction cost
     *  4. Increase production_rate for the affected resource
     *  5. Persist building record
     */
    @Transactional
    public BuildingResponse construct(Long colonyId, BuildingRequest request) {
        authService.requireColonyOwnership(colonyId);
        ColonyEntity colony = colonyRepository.findById(colonyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Colony not found: " + colonyId));

        if (!"ACTIVE".equals(colony.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Colony '%s' is not active".formatted(colony.getName()));
        }

        BuildingType definition = request.type();

        // Check MATERIALS balance
        ColonyResourceEntity materials = resourceRepository
                .findByColonyIdAndResourceType(colonyId, ResourceType.MATERIALS.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "MATERIALS resource not found for colony " + colonyId));

        if (materials.getCurrentAmount().compareTo(definition.getConstructionCost()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient MATERIALS: need %.0f, have %.2f"
                            .formatted(definition.getConstructionCost(),
                                    materials.getCurrentAmount()));
        }

        // Deduct construction cost
        materials.setCurrentAmount(
                materials.getCurrentAmount().subtract(definition.getConstructionCost()));
        resourceRepository.save(materials);

        // Boost production rate for affected resource
        ColonyResourceEntity affectedResource = resourceRepository
                .findByColonyIdAndResourceType(
                        colonyId, definition.getAffectedResource().name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "%s resource not found for colony %d"
                                .formatted(definition.getAffectedResource(), colonyId)));

        affectedResource.setProductionRate(
                affectedResource.getProductionRate().add(definition.getProductionBonus()));
        resourceRepository.save(affectedResource);

        // Persist building record
        LocalDateTime builtAt = colony.getLastTickAt();
        BuildingEntity building = BuildingEntity.builder()
                .colony(colony)
                .type(definition.name())
                .builtAt(builtAt)
                .build();

        building = buildingRepository.save(building);

        log.info("Building constructed: type={} colony='{}' | cost={} MATERIALS | +{} {}/tick",
                definition.name(), colony.getName(),
                definition.getConstructionCost(),
                definition.getProductionBonus(),
                definition.getAffectedResource());

        return toResponse(building, definition);
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> getBuildingsForColony(Long colonyId) {
        authService.requireColonyOwnership(colonyId);
        return buildingRepository.findAllByColonyId(colonyId)
                .stream()
                .map(entity -> {
                    BuildingType type = BuildingType.valueOf(entity.getType());
                    return toResponse(entity, type);
                })
                .toList();
    }

    /**
     * Returns the full building catalog — all types with costs and bonuses.
     * Useful for the frontend to show what can be built.
     */
    public List<BuildingResponse> getCatalog() {
        return Arrays.stream(BuildingType.values())
                .map(type -> new BuildingResponse(
                        null,
                        type.name(),
                        type.getAffectedResource().name(),
                        type.getProductionBonus(),
                        type.getConstructionCost(),
                        null
                ))
                .toList();
    }

    private BuildingResponse toResponse(BuildingEntity entity, BuildingType type) {
        return new BuildingResponse(
                entity.getId(),
                entity.getType(),
                type.getAffectedResource().name(),
                type.getProductionBonus(),
                type.getConstructionCost(),
                entity.getBuiltAt()
        );
    }
}
