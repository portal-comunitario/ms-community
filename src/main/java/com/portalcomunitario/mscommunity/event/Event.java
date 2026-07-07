package com.portalcomunitario.mscommunity.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventCategoria categoria;

    @Column(name = "agrupacion_id")
    private UUID agrupacionId;

    @Column(length = 60)
    private String subcategoria;

    @Column(length = 9)
    private String color;

    private Double latitud;
    private Double longitud;

    @Column(nullable = false)
    private boolean recurrente = false;

    @Column(length = 20)
    private String frecuencia;   // DIARIA | SEMANAL | MENSUAL | ANUAL

    private Integer intervalo;   // cada N

    @Column(name = "recurrencia_fin")
    private LocalDate recurrenciaFin;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Column(name = "author_nombre", length = 120)
    private String authorNombre;

    @Column(name = "notificado_comunidad")
    private java.time.LocalDateTime notificadoComunidad;

    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (categoria == null) {
            categoria = EventCategoria.GENERAL;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public EventCategoria getCategoria() { return categoria; }
    public void setCategoria(EventCategoria categoria) { this.categoria = categoria; }

    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID agrupacionId) { this.agrupacionId = agrupacionId; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public boolean isRecurrente() { return recurrente; }
    public void setRecurrente(boolean recurrente) { this.recurrente = recurrente; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public Integer getIntervalo() { return intervalo; }
    public void setIntervalo(Integer intervalo) { this.intervalo = intervalo; }

    public LocalDate getRecurrenciaFin() { return recurrenciaFin; }
    public void setRecurrenciaFin(LocalDate recurrenciaFin) { this.recurrenciaFin = recurrenciaFin; }

    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getAuthorNombre() { return authorNombre; }
    public void setAuthorNombre(String authorNombre) { this.authorNombre = authorNombre; }

    public java.time.LocalDateTime getNotificadoComunidad() { return notificadoComunidad; }
    public void setNotificadoComunidad(java.time.LocalDateTime v) { this.notificadoComunidad = v; }

    public boolean isRecordatorioEnviado() { return recordatorioEnviado; }
    public void setRecordatorioEnviado(boolean v) { this.recordatorioEnviado = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
