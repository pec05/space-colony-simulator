package com.peccio.space_colony_simulator.application.trade;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.TradeEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Settles in-transit trades during each simulation tick.
 * Called by CatchUpService after ticks are processed.
 *
 * Settlement rules:
 *   receiver ACTIVE   → resources added, trade = ARRIVED
 *   receiver DESTROYED/ABANDONED → resources lost, trade = FAILED
 */
@Slf4j
@Service
public class TradeSettlementService {

    private final TradeRepository tradeRepository;
    private final ColonyRepository         colonyRepository;
    private final ColonyResourceRepository resourceRepository;

    public TradeSettlementService(
            TradeRepository tradeRepository,
            ColonyRepository colonyRepository,
            ColonyResourceRepository resourceRepository) {

        this.tradeRepository    = tradeRepository;
        this.colonyRepository   = colonyRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Settles all trades that have reached their ETA by currentSimTime.
     *
     * @param colonyId       the receiver colony being processed
     * @param currentSimTime the simulation timestamp after this tick batch
     * @return number of trades settled (arrived or failed)
     */
    @Transactional
    public int settleArrivedTrades(Long colonyId, LocalDateTime currentSimTime) {
        List<TradeEntity> arrivedTrades = tradeRepository
                .findAllByReceiverColonyIdAndStatusAndEtaSimTimeLessThanEqual(
                        colonyId, "PENDING", currentSimTime);

        if (arrivedTrades.isEmpty()) return 0;

        ColonyEntity receiver = colonyRepository.findById(colonyId).orElseThrow();
        int settled = 0;

        for (TradeEntity trade : arrivedTrades) {
            if ("ACTIVE".equals(receiver.getStatus())) {
                deliverResources(trade, colonyId);
                trade.setStatus("ARRIVED");
                trade.setArrivedAt(LocalDateTime.now());

                log.info("Trade {} arrived: {} {} delivered to colony '{}'",
                        trade.getId(), trade.getAmount(),
                        trade.getResourceType(), receiver.getName());
            } else {
                trade.setStatus("FAILED");

                log.warn("Trade {} failed: receiver colony '{}' is {} — resources lost",
                        trade.getId(), receiver.getName(), receiver.getStatus());
            }

            tradeRepository.save(trade);
            settled++;
        }

        return settled;
    }

    private void deliverResources(TradeEntity trade, Long colonyId) {
        resourceRepository
                .findByColonyIdAndResourceType(colonyId, trade.getResourceType())
                .ifPresent(resource -> {
                    BigDecimal delivered = resource.getCurrentAmount()
                            .add(trade.getAmount())
                            .min(resource.getStorageCapacity()); // cap at storage
                    resource.setCurrentAmount(delivered);
                    resourceRepository.save(resource);
                });
    }
}
