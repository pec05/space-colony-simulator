package com.peccio.space_colony_simulator.application.replay;

import com.peccio.space_colony_simulator.domain.model.ColonyResource;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.mapper.ColonyMapper;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReplayService {

    private final ColonyRepository colonyRepository;
    private final ColonyEventRepository eventRepository;
    private final ColonyResourceRepository resourceRepository;
    private final ColonyMapper colonyMapper;

    public ReplayService(ColonyRepository colonyRepository,
                         ColonyEventRepository eventRepository,
                         ColonyResourceRepository resourceRepository,
                         ColonyMapper colonyMapper) {
        this.colonyRepository = colonyRepository;
        this.eventRepository = eventRepository;
        this.resourceRepository = resourceRepository;
        this.colonyMapper = colonyMapper;
    }

    // REPLAY — tick-by-tick event history

    /**
     * Returns all simulation ticks grouped by sim timestamp.
     * If 'since' is provided, only ticks after that point are included.
     * If 'since' is null, the full event history is returned.
     *
     * Ticks with no events are NOT included — only ticks where something happened.
     * The client can infer quiet ticks from the gap in tickNumbers.
     */
    @Transactional(readOnly = true)
    public ReplayResponse getReplay(Long colonyId, LocalDateTime since) {
        validateColonyExists(colonyId);

        List<ColonyEventEntity> events = (since != null)
                ? eventRepository.findAllByColonyIdAndSimOccurredAtAfterOrderBySimOccurredAtAsc(colonyId, since)
                : eventRepository.findAllByColonyIdOrderBySimOccurredAtAsc(colonyId);

        String colonyName = colonyRepository.findById(colonyId)
                .orElseThrow().getName();

        List<ReplayEntry> ticks = groupIntoTicks(events);

        log.debug("Replay for colony id={} — {} tick(s) with events, {} total event(s)",
                colonyId, ticks.size(), events.size());

        return new ReplayResponse(colonyId, colonyName, ticks.size(), events.size(), ticks);
    }

    // CURRENT STATE — just show me the result

    /**
     * Returns the current snapshot of a colony:
     * population, all resource levels, and all active (unresolved) events.
     */
    @Transactional(readOnly = true)
    public ColonyStateResponse getCurrentState(Long colonyId) {
        var colonyEntity = colonyRepository.findById(colonyId)
                .orElseThrow(() -> notFound(colonyId));

        List<ResourceStateDto> resources = resourceRepository
                .findAllByColonyId(colonyId)
                .stream()
                .map(this::toResourceDto)
                .toList();

        // Only the most recent unresolved event per type
        List<EventSummary> activeEvents = eventRepository
                .findAllByColonyIdAndResolved(colonyId, false)
                .stream()
                .collect(Collectors.toMap(
                        ColonyEventEntity::getEventType,
                        e -> e,
                        (existing, newer) -> newer
                ))
                .values()
                .stream()
                .map(this::toEventSummary)
                .toList();

        return new ColonyStateResponse(
                colonyEntity.getId(),
                colonyEntity.getName(),
                colonyEntity.getStatus(),
                colonyEntity.getPopulation(),
                colonyEntity.getLastTickAt(),
                colonyEntity.getLastProcessedAt(),
                resources,
                activeEvents
        );
    }

    /**
     * Groups a flat event list into ReplayEntry objects keyed by simOccurredAt.
     * Uses LinkedHashMap to preserve chronological order.
     */
    private List<ReplayEntry> groupIntoTicks(List<ColonyEventEntity> events) {
        Map<LocalDateTime, List<ColonyEventEntity>> grouped = events.stream()
                .collect(Collectors.groupingBy(
                        ColonyEventEntity::getSimOccurredAt,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ReplayEntry> ticks = new ArrayList<>();
        int tickNumber = 1;

        for (Map.Entry<LocalDateTime, List<ColonyEventEntity>> entry : grouped.entrySet()) {
            List<EventSummary> summaries = entry.getValue().stream()
                    .map(this::toEventSummary)
                    .toList();

            ticks.add(new ReplayEntry(tickNumber++, entry.getKey(), summaries));
        }

        return ticks;
    }

    private ResourceStateDto toResourceDto(ColonyResourceEntity entity) {
        ColonyResource domain = colonyMapper.toDomain(entity);
        return new ResourceStateDto(
                entity.getResourceType(),
                entity.getCurrentAmount(),
                entity.getProductionRate(),
                entity.getConsumptionRate(),
                entity.getStorageCapacity(),
                domain.isInShortage(),
                domain.isCriticallyLow()
        );
    }

    private EventSummary toEventSummary(ColonyEventEntity entity) {
        return new EventSummary(
                entity.getId(),
                entity.getEventType(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getSimOccurredAt(),
                entity.getSimResolvedAt(),
                entity.isResolved()
        );
    }

    private void validateColonyExists(Long colonyId) {
        if (!colonyRepository.existsById(colonyId)) {
            throw notFound(colonyId);
        }
    }

    private ResponseStatusException notFound(Long colonyId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Colony not found with id: " + colonyId
        );
    }
}
