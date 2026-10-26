package com.ritech.backend.integration;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, Integer> {

    List<IntegrationLog> findByServiceAndCreatedAtBetweenOrderByCreatedAtAsc(String service, LocalDateTime from,
            LocalDateTime to);
}
