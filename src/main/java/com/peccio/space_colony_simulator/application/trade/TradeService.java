package com.peccio.space_colony_simulator.application.trade;

import com.peccio.space_colony_simulator.application.auth.AuthService;
import com.peccio.space_colony_simulator.infrastructure.config.SimulationProperties;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.TradeEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.TradeRepository;
import com.peccio.space_colony_simulator.infrastructure.security.AuthenticatedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ColonyRepository colonyRepository;
    private final ColonyResourceRepository resourceRepository;
    private final SimulationProperties simulationProperties;
    private final AuthenticatedUserService authService;

    public TradeService(
            TradeRepository tradeRepository,
            ColonyRepository colonyRepository,
            ColonyResourceRepository resourceRepository,
            SimulationProperties simulationProperties, AuthenticatedUserService authService) {

        this.tradeRepository       = tradeRepository;
        this.colonyRepository      = colonyRepository;
        this.resourceRepository    = resourceRepository;
        this.simulationProperties  = simulationProperties;
        this.authService = authService;
    }
    // initial trade
    @Transactional
    public TradeResponse initiateTrade(Long senderColonyId, TradeRequest request) {
        authService.requireColonyOwnership(senderColonyId);
        ColonyEntity sender   = loadActiveColony(senderColonyId, "Sender");
        ColonyEntity receiver = loadActiveColony(request.receiverColonyId(), "Receiver");

        if (senderColonyId.equals(request.receiverColonyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A colony cannot trade with itself");
        }

        // Validate and deduct resources from sender
        ColonyResourceEntity senderResource = resourceRepository
                .findByColonyIdAndResourceType(senderColonyId, request.resourceType().name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resource %s not found in colony %d"
                                .formatted(request.resourceType(), senderColonyId)));

        if (senderResource.getCurrentAmount().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient %s: available=%.2f requested=%.2f"
                            .formatted(request.resourceType(),
                                    senderResource.getCurrentAmount(),
                                    request.amount()));
        }

        senderResource.setCurrentAmount(
                senderResource.getCurrentAmount().subtract(request.amount()));
        resourceRepository.save(senderResource);

        // Calculate ETA based on sender's current sim time
        LocalDateTime now = sender.getLastTickAt();
        LocalDateTime eta = now.plusHours(simulationProperties.getTradeTransitHours());

        TradeEntity trade = TradeEntity.builder()
                .senderColony(sender)
                .receiverColony(receiver)
                .resourceType(request.resourceType().name())
                .amount(request.amount())
                .status("PENDING")
                .initiatedSimTime(now)
                .etaSimTime(eta)
                .build();

        trade = tradeRepository.save(trade);

        log.info("Trade initiated: '{}' → '{}' | {} {} | ETA={}",
                sender.getName(), receiver.getName(),
                request.amount(), request.resourceType(), eta);

        return toResponse(trade);
    }


    // Cancel trade
    @Transactional
    public TradeResponse cancelTrade(Long senderColonyId, Long tradeId) {
        authService.requireColonyOwnership(senderColonyId);
        TradeEntity trade = tradeRepository
                .findByIdAndSenderColonyId(tradeId, senderColonyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trade %d not found for colony %d"
                                .formatted(tradeId, senderColonyId)));

        if (!"PENDING".equals(trade.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PENDING trades can be cancelled. Current status: "
                            + trade.getStatus());
        }

        // Return resources to sender
        resourceRepository
                .findByColonyIdAndResourceType(senderColonyId, trade.getResourceType())
                .ifPresent(resource -> {
                    BigDecimal returned = resource.getCurrentAmount()
                            .add(trade.getAmount())
                            .min(resource.getStorageCapacity());
                    resource.setCurrentAmount(returned);
                    resourceRepository.save(resource);
                    log.info("Trade {} cancelled — {} {} returned to colony id={}",
                            tradeId, trade.getAmount(), trade.getResourceType(), senderColonyId);
                });

        trade.setStatus("CANCELLED");
        tradeRepository.save(trade);

        return toResponse(trade);
    }


    // Queries
    @Transactional(readOnly = true)
    public List<TradeResponse> getTradesForColony(Long colonyId) {
        authService.requireColonyOwnership(colonyId);
        List<TradeEntity> sent     = tradeRepository.findAllBySenderColonyId(colonyId);
        List<TradeEntity> received = tradeRepository.findAllByReceiverColonyId(colonyId);

        return Stream.concat(sent.stream(), received.stream())
                .map(this::toResponse)
                .sorted(Comparator.comparing(TradeResponse::initiatedSimTime).reversed())
                .toList();
    }

    private ColonyEntity loadActiveColony(Long colonyId, String label) {
        ColonyEntity colony = colonyRepository.findById(colonyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "%s colony not found: %d".formatted(label, colonyId)));

        if (!"ACTIVE".equals(colony.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "%s colony '%s' is not active (status=%s)"
                            .formatted(label, colony.getName(), colony.getStatus()));
        }

        return colony;
    }

    private TradeResponse toResponse(TradeEntity trade) {
          return new TradeResponse(
                trade.getId(),
                trade.getSenderColony() != null ? trade.getSenderColony().getId()   : null,
                trade.getSenderColony() != null ? trade.getSenderColony().getName() : "unknown",
                trade.getReceiverColony() != null ? trade.getReceiverColony().getId()   : null,
                trade.getReceiverColony() != null ? trade.getReceiverColony().getName() : "unknown",
                trade.getResourceType(),
                trade.getAmount(),
                trade.getStatus(),
                trade.getInitiatedSimTime(),
                trade.getEtaSimTime(),
                trade.getArrivedAt()
        );
    }
}
