package com.ritech.backend.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer id;

    @Column(name = "id_usuario", nullable = false)
    private Integer userId;

    @Column(name = "accion", nullable = false, length = 100)
    private String action;

    @Column(name = "descripcion", columnDefinition = "text")
    private String description;

    @Column(name = "fecha_hora")
    private LocalDateTime createdAt;

    protected AuditEntry() {
    }

    public AuditEntry(Integer userId, String action, String description, LocalDateTime createdAt) {
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
