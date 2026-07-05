package com.portalcomunitario.mscommunity.agrupacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** Agrupación comunitaria genérica (club, taller, comité, centro, etc.). */
@Entity
@Table(name = "agrupaciones")
public class Agrupacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String responsable;

    @Column(name = "maneja_cuotas", nullable = false)
    private boolean manejaCuotas;

    /** Reunión semanal: 1=Lunes .. 7=Domingo (ISO). Null = sin reunión periódica. */
    @Column(name = "reunion_dia_semana")
    private Integer reunionDiaSemana;

    @Column(name = "reunion_hora")
    private LocalTime reunionHora;

    /** Pausa/vacaciones: rango en que no hay reuniones. */
    @Column(name = "pausa_inicio")
    private LocalDate pausaInicio;

    @Column(name = "pausa_fin")
    private LocalDate pausaFin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public boolean isManejaCuotas() { return manejaCuotas; }
    public void setManejaCuotas(boolean manejaCuotas) { this.manejaCuotas = manejaCuotas; }
    public Integer getReunionDiaSemana() { return reunionDiaSemana; }
    public void setReunionDiaSemana(Integer reunionDiaSemana) { this.reunionDiaSemana = reunionDiaSemana; }
    public LocalTime getReunionHora() { return reunionHora; }
    public void setReunionHora(LocalTime reunionHora) { this.reunionHora = reunionHora; }
    public LocalDate getPausaInicio() { return pausaInicio; }
    public void setPausaInicio(LocalDate pausaInicio) { this.pausaInicio = pausaInicio; }
    public LocalDate getPausaFin() { return pausaFin; }
    public void setPausaFin(LocalDate pausaFin) { this.pausaFin = pausaFin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
