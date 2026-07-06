package com.portalcomunitario.mscommunity.asistencia;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "asistencias",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "vecino_email"}))
public class Asistencia {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "agrupacion_id", nullable = false)
    private UUID agrupacionId;

    @Column(name = "vecino_email", nullable = false)
    private String vecinoEmail;

    @Column(nullable = false)
    private boolean presente;

    public UUID getId() { return id; }
    public UUID getEventoId() { return eventoId; }
    public void setEventoId(UUID v) { this.eventoId = v; }
    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID v) { this.agrupacionId = v; }
    public String getVecinoEmail() { return vecinoEmail; }
    public void setVecinoEmail(String v) { this.vecinoEmail = v; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean v) { this.presente = v; }
}
