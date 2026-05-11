package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColonyEventRepository extends JpaRepository<ColonyEventEntity, Long> {
    List<ColonyEventEntity> findAllByColonyId(Long colonyId);
    List<ColonyEventEntity> findAllByColonyIdAndResolved(Long colonyId, boolean resolved);
}
