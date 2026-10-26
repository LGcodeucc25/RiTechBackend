package com.ritech.backend.alert;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ritech.backend.analysis.Recommendation;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findFirstByEmittedAtLessThanEqualOrderByEmittedAtDescIdDesc(LocalDateTime time);

    Optional<Alert> findFirstByOrderByEmittedAtDescIdDesc();

    Page<Alert> findByRecommendationInAndEmittedAtBetween(Collection<Recommendation> types, LocalDateTime from,
            LocalDateTime to, Pageable pageable);

    List<Alert> findByEmittedAtBetweenOrderByEmittedAtAsc(LocalDateTime from, LocalDateTime to);

    List<Alert> findByMarketDataIdIn(Collection<Integer> marketDataIds);
}
