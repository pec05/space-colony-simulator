package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.replay.ColonyStateResponse;
import com.peccio.space_colony_simulator.application.replay.ReplayResponse;
import com.peccio.space_colony_simulator.application.replay.ReplayService;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Exposes colony state and replay history.
 */
@Slf4j
@RestController
@RequestMapping("/api/colonies")
public class ColonyStateController {

    private final ReplayService replayService;
    private final AuthenticatedUserService authService;

    public ColonyStateController(ReplayService replayService, AuthenticatedUserService authService) {
        this.replayService = replayService;
        this.authService = authService;
    }

    /**
     * Returns the current state of a colony.
     * Use this for the "just show me the result" path.
     *
     * GET /api/colonies/{id}/state
     */
    @GetMapping("/{id}/state")
    public ResponseEntity<ColonyStateResponse> getState(@PathVariable Long id) {
        log.debug("State requested for colony id={}", id);
        authService.requireColonyOwnership(id);
        return ResponseEntity.ok(replayService.getCurrentState(id));
    }

    /**
     * Returns tick-by-tick event history for a colony.
     * Optional 'since' param filters events after a sim timestamp.
     *
     * GET /api/colonies/{id}/replay
     * GET /api/colonies/{id}/replay?since=2350-01-01T00:00:00
     */
    @GetMapping("/{id}/replay")
    public ResponseEntity<ReplayResponse> getReplay(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        log.debug("Replay requested for colony id={} since={}", id, since);
        authService.requireColonyOwnership(id);
        return ResponseEntity.ok(replayService.getReplay(id, since));
    }


}
