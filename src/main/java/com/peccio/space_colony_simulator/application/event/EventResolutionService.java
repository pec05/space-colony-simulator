package com.peccio.space_colony_simulator.application.event;

import com.peccio.space_colony_simulator.domain.model.Colony;
import com.peccio.space_colony_simulator.domain.model.EventSeverity;
import com.peccio.space_colony_simulator.domain.model.ResourceType;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Resolves colony events — both automatically and manually.
 *
 * AUTO-RESOLUTION rules (called after every tick batch):
 *   HIGH severity (depleted)      → resolve when currentAmount > 0
 *   LOW severity (critically low) → resolve when above 10% capacity
 *
 * MANUAL RESOLUTION:
 *   Player explicitly acknowledges and closes an event.
 *   Allowed even if condition still exists (player choice).
 */
@Slf4j
@Service
public class EventResolutionService {

    private final ColonyEventRepository eventRepository;
    private final AuthenticatedUserService authService;

    public EventResolutionService(ColonyEventRepository eventRepository, AuthenticatedUserService authService) {
        this.eventRepository = eventRepository;
        this.authService = authService;
    }

    // Auto-resolution — called by CatchUpService after ticks

    /**
     * Checks all active events for this colony.
     * Resolves any whose triggering condition no longer exists.
     *
     * @param colonyId  the colony being processed
     * @param colony    domain object with current (post-tick) resource state
     * @param simTime   simulation timestamp to stamp on resolved events
     * @return number of events auto-resolved
     */
    @Transactional
    public int autoResolve(Long colonyId, Colony colony, LocalDateTime simTime) {
        List<ColonyEventEntity> activeEvents =
                eventRepository.findAllByColonyIdAndResolved(colonyId, false);

        if (activeEvents.isEmpty()) return 0;

        int resolvedCount = 0;

        for (ColonyEventEntity event : activeEvents) {
            if (conditionCleared(event, colony)) {
                event.setResolved(true);
                event.setSimResolvedAt(simTime);
                eventRepository.save(event);
                resolvedCount++;

                log.info("Auto-resolved: colony='{}' event={} type={} severity={}",
                        colony.getName(),
                        event.getId(),
                        event.getEventType(),
                        event.getSeverity());
            }
        }

        if (resolvedCount > 0) {
            log.info("Colony '{}' — {} event(s) auto-resolved at simTime={}",
                    colony.getName(), resolvedCount, simTime);
        }

        return resolvedCount;
    }

    /**
     * Player manually resolves an event.
     * Validates ownership (event belongs to colony) before resolving.
     */
    @Transactional
    public void manualResolve(Long colonyId, Long eventId) {
        authService.requireColonyOwnership(colonyId);
        ColonyEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found: " + eventId));

        if (!event.getColony().getId().equals(colonyId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event %d does not belong to colony %d".formatted(eventId, colonyId));
        }

        if (event.isResolved()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event %d is already resolved".formatted(eventId));
        }

        event.setResolved(true);
        event.setSimResolvedAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Manually resolved: colonyId={} eventId={} type={}",
                colonyId, eventId, event.getEventType());
    }

    /**
     * Returns true if the condition that caused this event no longer exists.
     */
    private boolean conditionCleared(ColonyEventEntity event, Colony colony) {
        ResourceType resourceType = resolveResourceType(event.getEventType());
        if (resourceType == null) return false;

        return colony.getResource(resourceType)
                .map(resource -> {
                    boolean isHighSeverity = EventSeverity.HIGH.name()
                            .equals(event.getSeverity());

                    // HIGH (depleted) → clear when no longer in shortage
                    if (isHighSeverity) {
                        return !resource.isInShortage();
                    }

                    // LOW (critically low) → clear when above 10% threshold
                    return !resource.isCriticallyLow();
                })
                .orElse(false);
    }

    /**
     * Maps an EventType string to its corresponding ResourceType.
     * Returns null for non-resource events (e.g. CATASTROPHE).
     */
    private ResourceType resolveResourceType(String eventType) {
        return switch (eventType) {
            case "OXYGEN_SHORTAGE"   -> ResourceType.OXYGEN;
            case "FOOD_SHORTAGE"     -> ResourceType.FOOD;
            case "ENERGY_FAILURE"    -> ResourceType.ENERGY;
            case "MATERIAL_SHORTAGE" -> ResourceType.MATERIALS;
            default                  -> null;
        };
    }
}
