package com.ritech.backend.simulation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRepository extends JpaRepository<Simulation, Integer> {

    List<Simulation> findByUserIdOrderByCreatedAtDescIdDesc(Integer userId);

    Optional<Simulation> findByIdAndUserId(Integer id, Integer userId);
}
