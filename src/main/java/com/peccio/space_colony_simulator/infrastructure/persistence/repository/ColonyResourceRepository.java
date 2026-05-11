package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ColonyResourceRepository extends JpaRepository<ColonyResourceEntity, Long> {

    List<ColonyResourceEntity> findAllByColonyId(Long colonyId);
    Optional<ColonyResourceEntity> findByColonyIdAndResourceType(Long colonyId, String resourceType);
}
