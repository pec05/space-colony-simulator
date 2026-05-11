package com.peccio.space_colony_simulator.infrastructure.persistence.mapper;

import com.peccio.space_colony_simulator.domain.model.*;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ColonyMapper {

    /*
    Entity -> Domain
     */

    public Colony toDomain(ColonyEntity entity) {
        List<ColonyResource> resources = entity.getResources().stream()
                .map((ColonyResourceEntity r) -> toDomain(r))
                .toList();

        return Colony.builder()
                .id(entity.getId())
                .name(entity.getName())
                .ownerId(entity.getOwnerId())
                .population(entity.getPopulation())
                .status(ColonyStatus.valueOf(entity.getStatus()))
                .foundedAt(entity.getFoundedAt())
                .lastTickAt(entity.getLastTickAt())
                .resources(resources)
                .build();
    }


    public ColonyResource toDomain(ColonyResourceEntity entity) {
        return ColonyResource.builder()
                .id(entity.getId())
                .colonyId(entity.getColony().getId())
                .resourceType(ResourceType.valueOf(entity.getResourceType()))
                .currentAmount(entity.getCurrentAmount())
                .productionRate(entity.getProductionRate())
                .consumptionRate(entity.getConsumptionRate())
                .storageCapacity(entity.getStorageCapacity())
                .build();
    }

    public ColonyEvent toDomain(ColonyEventEntity entity) {
        return ColonyEvent.builder()
                .id(entity.getId())
                .colonyId(entity.getColony().getId())
                .eventType(EventType.valueOf(entity.getEventType()))
                .severity(EventSeverity.valueOf(entity.getSeverity()))
                .description(entity.getDescription())
                .simOccuredAt(entity.getSimOccurredAt())
                .simResolvedAt(entity.getSimResolvedAt())
                .resolved(entity.isResolved())
                .build();
    }

    /*
    Doamin -> Entity
     */

    public void updateEntity(ColonyEntity entity, Colony domain) {
        entity.setPopulation(domain.getPopulation());
        entity.setStatus(domain.getStatus().name());
        entity.setLastTickAt(domain.getLastTickAt());
    }

    public void updateEntity(ColonyResourceEntity entity, ColonyResource domain) {
        entity.setCurrentAmount(domain.getCurrentAmount());
        entity.setProductionRate(domain.getProductionRate());
        entity.setConsumptionRate(domain.getConsumptionRate());
        entity.setStorageCapacity(domain.getStorageCapacity());
    }

    public void updateEntity(ColonyEventEntity entity, ColonyEvent domain) {
        entity.setResolved(domain.isResolved());
        entity.setSimResolvedAt(domain.getSimResolvedAt());
    }


}
