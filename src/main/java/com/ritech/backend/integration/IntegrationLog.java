package com.ritech.backend.integration;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "log_integracion")
public class IntegrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Integer id;

    @Column(name = "servicio", nullable = false, length = 100)
    private String service;

    @Column(name = "estado", nullable = false, length = 50)
    private String status;

    @Column(name = "mensaje_error", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "fecha_hora")
    private LocalDateTime createdAt;

    protected IntegrationLog() {
    }

    public IntegrationLog(String service, String status, String errorMessage, LocalDateTime createdAt) {
        this.service = service;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
