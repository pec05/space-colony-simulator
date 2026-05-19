package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
    List<TradeEntity> findAllBySenderColonyId(Long senderColonyId);

    List<TradeEntity> findAllByReceiverColonyId(Long receiverColonyId);

    List<TradeEntity> findAllBySenderColonyIdAndStatus(Long senderColonyId, String status);

    List<TradeEntity> findAllByReceiverColonyIdAndStatusAndEtaSimTimeLessThanEqual(
            Long receiverColonyId, String status, LocalDateTime simTime);

    Optional<TradeEntity> findByIdAndSenderColonyId(Long id, Long senderColonyId);
}
