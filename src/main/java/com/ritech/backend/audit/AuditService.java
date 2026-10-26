package com.ritech.backend.audit;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ritech.backend.common.DateTimes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;
    private final Clock clock;

    public AuditService(AuditRepository auditRepository, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    /** Records an event for a known user. An audit failure never breaks the request. */
    public void record(Integer userId, String action, String description) {
        try {
            auditRepository.save(new AuditEntry(userId, action, description, DateTimes.nowUtc(clock)));
        } catch (RuntimeException ex) {
            log.error("Could not store audit event {} for user {}", action, userId, ex);
        }
    }
}
