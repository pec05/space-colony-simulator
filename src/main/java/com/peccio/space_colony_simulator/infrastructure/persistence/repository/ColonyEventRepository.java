package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ColonyEventRepository extends JpaRepository<ColonyEventEntity, Long> {
    List<ColonyEventEntity> findAllByColonyId(Long colonyId);
    List<ColonyEventEntity> findAllByColonyIdAndResolved(Long colonyId, boolean resolved);

    // Returns ALL events for a colony ordered by sim time — used for full replay
    List<ColonyEventEntity> findAllByColonyIdOrderBySimOccurredAtAsc(Long colonyId);

    // Returns events AFTER a given sim timestamp — used for partial replay (since last login)
    List<ColonyEventEntity> findAllByColonyIdAndSimOccurredAtAfterOrderBySimOccurredAtAsc(
            Long colonyId, LocalDateTime since);

    Optional<ColonyEventEntity> findByColonyIdAndEventTypeAndResolved(
            Long colonyId, String eventType, boolean resolved);
}
