package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.Colony;
import com.peccio.space_colony_simulator.domain.model.ColonyEvent;
import com.peccio.space_colony_simulator.domain.model.ColonyResource;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.mapper.ColonyMapper;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the full tick lifecycle for a SINGLE colony.
 *
 * Each call runs in its own transaction (@Transactional).
 * This means a failure in one colony never rolls back another colony's progress.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ColonyTickService {

    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;
    private final ColonyEventRepository eventRepository;
    private final ColonyMapper colonyMapper;
    private final TickProcessor tickProcessor;

    /**
     * Processes one simulation tick for the given colony entity.
     * Steps:
     *  1. Map entity → domain
     *  2. Compute next sim timestamp
     *  3. Run TickProcessor (pure logic)
     *  4. Persist resource changes
     *  5. Persist new events (no duplicates)
     *  6. Advance colony's lastTickAt
     */
    @Transactional
    public void tick(ColonyEntity colonyEntity) {
        Colony colony = colonyMapper.toDomain(colonyEntity);
        LocalDateTime next = colony.getLastTickAt().plusHours(1);

        TickResult result = tickProcessor.process(colony, next);

        persistResourceChanges(colony);
        persistNewEvents(result, colonyEntity);

        colony.advanceLastTickAt(next);
        colonyMapper.updateEntity(colonyEntity, colony);
        colonyRepository.save(colonyEntity);

        log.info("Colony '{}' ticked → simTime={} | events={}",
                colony.getName(), next, result.generatedEvents().size());
    }

    private void persistResourceChanges(Colony colony) {
        for (ColonyResource resource : colony.getResources()) {
            resourceRepository
                    .findByColonyIdAndResourceType(
                            colony.getId(),
                            resource.getResourceType().name())
                    .ifPresentOrElse(
                            entity -> {
                                colonyMapper.updateEntity(entity, resource);
                                resourceRepository.save(entity);
                            },
                            () -> log.warn(
                                    "Resource {} not found for colony id={}",
                                    resource.getResourceType(), colony.getId())
                    );
        }
    }

    private void persistNewEvents(TickResult result, ColonyEntity colonyEntity) {
        if (!result.hasEvent()) return;

        // Load existing unresolved event types to prevent duplicates
        Set<String> existingTypes = eventRepository
                .findAllByColonyIdAndResolved(colonyEntity.getId(), false)
                .stream()
                .map(ColonyEventEntity::getEventType)
                .collect(Collectors.toSet());

        List<ColonyEventEntity> toSave = result.generatedEvents().stream()
                .filter(event -> !existingTypes.contains(event.getEventType().name()))
                .map(event -> toEntity(event, colonyEntity))
                .toList();

        if (!toSave.isEmpty()) {
            eventRepository.saveAll(toSave);
            log.info("Colony '{}' — {} new event(s) persisted",
                    colonyEntity.getName(), toSave.size());
        }
    }

    private ColonyEventEntity toEntity(ColonyEvent event, ColonyEntity colonyEntity) {
        return ColonyEventEntity.builder()
                .colony(colonyEntity)
                .eventType(event.getEventType().name())
                .severity(event.getSeverity().name())
                .description(event.getDescription())
                .simOccurredAt(event.getSimOccurredAt())
                .resolved(false)
                .build();
    }
}
