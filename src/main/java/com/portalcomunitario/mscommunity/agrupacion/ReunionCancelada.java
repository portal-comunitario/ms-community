package com.portalcomunitario.mscommunity.agrupacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reuniones_canceladas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"agrupacion_id", "fecha"}))
public class ReunionCancelada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agrupacion_id", nullable = false)
    private UUID agrupacionId;

    @Column(nullable = false)
    private LocalDate fecha;

    public UUID getId() { return id; }
    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID agrupacionId) { this.agrupacionId = agrupacionId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
