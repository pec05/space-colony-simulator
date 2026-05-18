package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.application.event.EventResolutionService;
import com.peccio.space_colony_simulator.domain.model.Colony;
import com.peccio.space_colony_simulator.domain.model.ColonyEvent;
import com.peccio.space_colony_simulator.domain.model.ColonyResource;
import com.peccio.space_colony_simulator.infrastructure.config.SimulationProperties;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.mapper.ColonyMapper;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles catch-up ticks for colonies that missed simulation time.
 *
 */
@Slf4j
@Service
public class CatchUpService {
    static final int MAX_CATCH_UP_TICKS = 100;

    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;
    private final ColonyEventRepository eventRepository;
    private final ColonyMapper colonyMapper;
    private final TickProcessor            tickProcessor;
    private final SimulationProperties simulationProperties;
    private final EventResolutionService eventResolutionService;

    public CatchUpService(
            ColonyRepository colonyRepository,
            ColonyResourceRepository resourceRepository,
            ColonyEventRepository eventRepository,
            ColonyMapper colonyMapper,
            TickProcessor tickProcessor,
            SimulationProperties simulationProperties,
            EventResolutionService eventResolutionService) {

        this.colonyRepository    = colonyRepository;
        this.resourceRepository  = resourceRepository;
        this.eventRepository     = eventRepository;
        this.colonyMapper        = colonyMapper;
        this.tickProcessor       = tickProcessor;
        this.simulationProperties = simulationProperties;
        this.eventResolutionService = eventResolutionService;
    }

    /**
     * Main entry point.
     * Calculates missed ticks, runs them all in memory, persists once.
     */
    @Transactional
    public CatchUpResult catchUp(Long colonyId) {

        // Load fresh within THIS transaction — lazy collections work correctly
        ColonyEntity colonyEntity = colonyRepository.findById(colonyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colony not found: " + colonyId));

        LocalDateTime now        = LocalDateTime.now();
        int ticksToRun           = calculateMissedTicks(colonyEntity.getLastProcessedAt(), now);
        Colony colony            = colonyMapper.toDomain(colonyEntity);
        List<ColonyEvent> events = new ArrayList<>();

        // Run all missed ticks in memory — no DB access inside this loop
        for (int i = 0; i < ticksToRun; i++) {
            LocalDateTime nextSimTime = colony.getLastTickAt()
                    .plusHours(simulationProperties.getSimHoursPerTick());

            TickResult result = tickProcessor.process(colony, nextSimTime);
            events.addAll(result.generatedEvents());
            colony.advanceLastTickAt(nextSimTime);
        }

        // Persist everything once
        persistResourceChanges(colony);
        persistEvents(events, colonyEntity);

        // Auto-resolve events whose condition cleared after this tick batch
        int autoResolved = eventResolutionService.autoResolve(
                colonyId, colony, colony.getLastTickAt());

        if (autoResolved > 0) {
            log.info("Colony '{}' — {} event(s) auto-resolved", colony.getName(), autoResolved);
        }

        colonyMapper.updateEntity(colonyEntity, colony);
        colonyEntity.setLastProcessedAt(now);
        colonyRepository.save(colonyEntity);

        log.info("Colony '{}' — {} tick(s) processed, {} event(s) generated, {} auto-resolved",
                colony.getName(), ticksToRun, events.size(), autoResolved);

        return new CatchUpResult(colony.getId(), colony.getName(), ticksToRun, events);
    }

    /**
     * Calculates how many ticks to run based on real elapsed time.
     * Always runs at least 1 tick (normal scheduler fire).
     * Caps at MAX_CATCH_UP_TICKS.
     */
    private int calculateMissedTicks(LocalDateTime lastProcessedAt, LocalDateTime now) {
        long elapsedMs  = Duration.between(lastProcessedAt, now).toMillis();
        long missed     = elapsedMs / simulationProperties.getRateMs();
        int  ticks      = (int) Math.max(1, Math.min(missed, MAX_CATCH_UP_TICKS));

        if (missed > MAX_CATCH_UP_TICKS) {
            log.warn("Colony missed {} tick(s) — capped at {}. " +
                            "Consider reducing rate-ms or increasing the cap.",
                    missed, MAX_CATCH_UP_TICKS);
        }

        return ticks;
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
                            () -> log.warn("Resource {} not found for colony id={}",
                                    resource.getResourceType(), colony.getId())
                    );
        }
    }

    private void persistEvents(List<ColonyEvent> events, ColonyEntity colonyEntity) {
        if (events.isEmpty()) return;

        // Events already in DB (from previous runs)
        Set<String> existingTypes = eventRepository
                .findAllByColonyIdAndResolved(colonyEntity.getId(), false)
                .stream()
                .map(ColonyEventEntity::getEventType)
                .collect(Collectors.toSet());

        // Deduplicate WITHIN this catch-up run too — keep only first per type
        Set<String> seenInThisRun = new HashSet<>(existingTypes);

        List<ColonyEventEntity> toSave = new ArrayList<>();
        for (ColonyEvent event : events) {
            String type = event.getEventType().name();
            if (!seenInThisRun.contains(type)) {
                toSave.add(toEntity(event, colonyEntity));
                seenInThisRun.add(type);   // block duplicates within same run
            }
        }

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
