package com.ritech.backend.market;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketDataRepository extends JpaRepository<MarketData, Integer> {

    boolean existsByWindowEnd(LocalDateTime windowEnd);

    Optional<MarketData> findFirstByOrderByWindowEndDescIdDesc();

    List<MarketData> findByWindowEndBetweenOrderByWindowEndAsc(LocalDateTime from, LocalDateTime to);
}
