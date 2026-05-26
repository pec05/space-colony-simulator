package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.event.EventResolutionService;
import com.peccio.space_colony_simulator.application.replay.EventSummary;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Event management endpoints for a colony.
 * Supports listing and manual resolution.
 */
@Slf4j
@RestController
@RequestMapping("/api/colonies")
public class EventController {

    private final EventResolutionService eventResolutionService;
    private final ColonyEventRepository colonyEventRepository;
    private final AuthenticatedUserService authService;

    public EventController(EventResolutionService eventResolutionService, ColonyEventRepository colonyEventRepository, AuthenticatedUserService authService) {
        this.eventResolutionService = eventResolutionService;
        this.colonyEventRepository = colonyEventRepository;
        this.authService = authService;
    }

    /**
     * GET /api/colonies/{colonyId}/events
     * GET /api/colonies/{colonyId}/events?resolved=false
     */
    @GetMapping("/{colonyId}/events")
    public ResponseEntity<List<EventSummary>> getEvents(
            @PathVariable Long colonyId,
            @RequestParam(required = false) Boolean resolved) {

        authService.requireColonyOwnership(colonyId);

        List<ColonyEventEntity> entities = (resolved != null)
                ? colonyEventRepository.findAllByColonyIdAndResolved(colonyId, resolved)
                : colonyEventRepository.findAllByColonyIdOrderBySimOccurredAtAsc(colonyId);

        List<EventSummary> summaries = entities.stream()
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * PATCH /api/colonies/{colonyId}/events/{eventId}/resolve
     */
    @PatchMapping("/{colonyId}/events/{eventId}/resolve")
    public ResponseEntity<Void> resolveEvent(
            @PathVariable Long colonyId,
            @PathVariable Long eventId) {
        authService.requireColonyOwnership(colonyId);
        eventResolutionService.manualResolve(colonyId, eventId);
        return ResponseEntity.noContent().build();
    }

    private EventSummary toSummary(ColonyEventEntity entity) {
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
}
