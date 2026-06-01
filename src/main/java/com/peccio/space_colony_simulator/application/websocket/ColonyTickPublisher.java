package com.peccio.space_colony_simulator.application.websocket;

import com.peccio.space_colony_simulator.application.replay.ColonyStateResponse;
import com.peccio.space_colony_simulator.application.replay.ReplayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes colony state updates to WebSocket subscribers after each tick.
 *
 * Topic pattern: /topic/colonies/{colonyId}
 * Payload: full ColonyStateResponse (resources, events, population)
 *
 * Called AFTER the tick transaction commits so clients always
 * receive the final persisted state — never intermediate state.
 */
@Slf4j
@Component
public class ColonyTickPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReplayService replayService;

    public ColonyTickPublisher(SimpMessagingTemplate messagingTemplate, ReplayService replayService) {
        this.messagingTemplate = messagingTemplate;
        this.replayService = replayService;
    }

    /**
     * Reads current colony state and broadcasts it to all subscribers.
     * Errors are caught and logged — a publish failure must never
     * affect the simulation engine.
     */
    public void publishColonyUpdate(Long colonyId) {
        try {
            ColonyStateResponse state = replayService.getCurrentState(colonyId);
            String destination = "/topic/colonies/" + colonyId;

            messagingTemplate.convertAndSend(destination, state);

            log.debug("WebSocket update published → {} | lastTickAt={}",
                    destination, state.lastTickAt());

        } catch (Exception e) {
            log.warn("WebSocket publish failed for colony id={}: {}",
                    colonyId, e.getMessage());
        }
    }
}
