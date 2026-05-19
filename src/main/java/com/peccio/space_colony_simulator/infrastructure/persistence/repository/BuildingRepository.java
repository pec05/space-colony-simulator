package com.peccio.space_colony_simulator.infrastructure.persistence.repository;

import com.peccio.space_colony_simulator.infrastructure.persistence.entity.BuildingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<BuildingEntity, Long> {

    List<BuildingEntity> findAllByColonyId(Long id);

    int countByColonyIdAndType(Long id, String type);
}
