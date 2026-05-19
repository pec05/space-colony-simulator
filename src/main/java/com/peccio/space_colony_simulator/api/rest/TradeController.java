package com.peccio.space_colony_simulator.api.rest;

import com.peccio.space_colony_simulator.application.trade.TradeRequest;
import com.peccio.space_colony_simulator.application.trade.TradeResponse;
import com.peccio.space_colony_simulator.application.trade.TradeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/colonies")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Initiate a trade from this colony to another.
     * POST /api/colonies/{colonyId}/trades
     */
    @PostMapping("/{colonyId}/trades")
    public ResponseEntity<TradeResponse> initiateTrade(
            @PathVariable Long colonyId,
            @RequestBody @Valid TradeRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tradeService.initiateTrade(colonyId, request));
    }

    /**
     * List all trades (sent and received) for a colony.
     * GET /api/colonies/{colonyId}/trades
     */
    @GetMapping("/{colonyId}/trades")
    public ResponseEntity<List<TradeResponse>> getTrades(@PathVariable Long colonyId) {
        return ResponseEntity.ok(tradeService.getTradesForColony(colonyId));
    }

    /**
     * Cancel a pending trade — resources returned to sender.
     * PATCH /api/colonies/{colonyId}/trades/{tradeId}/cancel
     */
    @PatchMapping("/{colonyId}/trades/{tradeId}/cancel")
    public ResponseEntity<TradeResponse> cancelTrade(
            @PathVariable Long colonyId,
            @PathVariable Long tradeId) {

        return ResponseEntity.ok(tradeService.cancelTrade(colonyId, tradeId));
    }
}
