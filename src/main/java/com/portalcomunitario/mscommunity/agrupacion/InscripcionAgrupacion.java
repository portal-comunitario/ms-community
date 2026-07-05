package com.portalcomunitario.mscommunity.agrupacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

/** Un vecino inscrito como socio de una agrupación. */
@Entity
@Table(name = "inscripciones_agrupacion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agrupacion_id", "vecino_email"}))
public class InscripcionAgrupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agrupacion_id", nullable = false)
    private UUID agrupacionId;

    @Column(name = "vecino_email", nullable = false)
    private String vecinoEmail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID agrupacionId) { this.agrupacionId = agrupacionId; }
    public String getVecinoEmail() { return vecinoEmail; }
    public void setVecinoEmail(String vecinoEmail) { this.vecinoEmail = vecinoEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
