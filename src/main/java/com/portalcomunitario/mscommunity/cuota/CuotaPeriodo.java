package com.portalcomunitario.mscommunity.cuota;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Configuración de un período de cuotas de una agrupación. */
@Entity
@Table(name = "cuota_periodos")
public class CuotaPeriodo {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agrupacion_id", nullable = false)
    private UUID agrupacionId;

    @Column(nullable = false)
    private Integer monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicidad periodicidad;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPeriodo estado;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (estado == null) estado = EstadoPeriodo.ABIERTA;
    }

    public UUID getId() { return id; }
    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID v) { this.agrupacionId = v; }
    public Integer getMonto() { return monto; }
    public void setMonto(Integer v) { this.monto = v; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(Periodicidad v) { this.periodicidad = v; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate v) { this.fechaInicio = v; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate v) { this.fechaFin = v; }
    public EstadoPeriodo getEstado() { return estado; }
    public void setEstado(EstadoPeriodo v) { this.estado = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
