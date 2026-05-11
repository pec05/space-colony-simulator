package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColonyRepository extends JpaRepository<ColonyEntity, Long> {

    List<ColonyEntity> findAllByStatus(String status);
}
