package com.portalcomunitario.mscommunity.cuota;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Control de idempotencia de los recordatorios de cuota pendiente ("nivel de deuda").
 * Guarda, por socio y período, cuántas cuotas pendientes se le notificaron por última vez.
 * Solo se vuelve a notificar si la deuda crece a un nivel mayor.
 */
@Entity
@Table(name = "recordatorio_cuota",
        uniqueConstraints = @UniqueConstraint(columnNames = {"periodo_id", "vecino_email"}))
public class RecordatorioCuota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "periodo_id", nullable = false)
    private UUID periodoId;

    @Column(name = "vecino_email", nullable = false)
    private String vecinoEmail;

    @Column(name = "nivel_notificado", nullable = false)
    private int nivelNotificado;

    @Column(name = "ultima_fecha")
    private LocalDate ultimaFecha;

    public UUID getId() { return id; }
    public UUID getPeriodoId() { return periodoId; }
    public void setPeriodoId(UUID v) { this.periodoId = v; }
    public String getVecinoEmail() { return vecinoEmail; }
    public void setVecinoEmail(String v) { this.vecinoEmail = v; }
    public int getNivelNotificado() { return nivelNotificado; }
    public void setNivelNotificado(int v) { this.nivelNotificado = v; }
    public LocalDate getUltimaFecha() { return ultimaFecha; }
    public void setUltimaFecha(LocalDate v) { this.ultimaFecha = v; }
}
