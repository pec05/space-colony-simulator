package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure simulation logic — no database, no transactions, no side effects.
 *
 * Responsibilities:
 *  1. Apply one tick to every resource in the colony
 *  2. Evaluate post-tick state and generate events if needed
 *
 * Rule: a shortage event is only generated once per resource type.
 * Duplicate-prevention is handled upstream in ColonyTickService.
 */

@Slf4j
@Component
public class TickProcessor {

    /**
     * Processes one simulation tick for the given colony.
     *
     * @param colony  the colony to tick (will be mutated in place)
     * @param simTime the simulation timestamp of this tick
     * @return TickResult containing any events generated
     */
    public TickResult process(Colony colony, LocalDateTime simTime) {
        List<ColonyEvent> events = new ArrayList<>();

        for (ColonyResource resource : colony.getResources()) {
            resource.applyTick();

            evaluateResource(colony, resource, simTime)
                    .ifPresent(events::add);
        }

        log.debug("Colony '{}' — tick at simTime={} — {} event(s) generated",
                colony.getName(), simTime, events.size());

        return new TickResult(colony.getId(), events);
    }

    private Optional<ColonyEvent> evaluateResource(Colony colony, ColonyResource resource, LocalDateTime simTime) {
        if (resource.isInShortage()) {
            return java.util.Optional.of(
                    buildEvent(colony, resource, simTime, EventSeverity.HIGH)
            );
        }

        if (resource.isCriticallyLow()) {
            return java.util.Optional.of(
                    buildEvent(colony, resource, simTime, EventSeverity.LOW)
            );
        }

        return java.util.Optional.empty();
    }

    private ColonyEvent buildEvent(
            Colony colony,
            ColonyResource resource,
            LocalDateTime simTime,
            EventSeverity severity) {

        EventType eventType = resolveEventType(resource.getResourceType());
        String description  = buildDescription(resource, severity);

        return ColonyEvent.builder()
                .colonyId(colony.getId())
                .eventType(eventType)
                .severity(severity)
                .description(description)
                .simOccurredAt(simTime)
                .resolved(false)
                .build();
    }

    private EventType resolveEventType(ResourceType type) {
        return switch (type) {
            case OXYGEN    -> EventType.OXYGEN_SHORTAGE;
            case FOOD      -> EventType.FOOD_SHORTAGE;
            case ENERGY    -> EventType.ENERGY_FAILURE;
            case MATERIALS -> EventType.MATERIAL_SHORTAGE;
        };
    }

    private String buildDescription(ColonyResource resource, EventSeverity severity) {
        return switch (severity) {
            case HIGH -> String.format(
                    "%s completely depleted. Colony survival is at risk.",
                    resource.getResourceType().name()
            );
            case LOW -> String.format(
                    "%s critically low — %.2f units remaining.",
                    resource.getResourceType().name(),
                    resource.getCurrentAmount()
            );
            default -> String.format(
                    "%s levels are below normal.",
                    resource.getResourceType().name()
            );
        };
    }

}
