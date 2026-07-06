package com.portalcomunitario.mscommunity.cuota;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Una cuota puntual de un socio en un período. */
@Entity
@Table(name = "cuotas")
public class Cuota {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "periodo_id", nullable = false)
    private UUID periodoId;

    @Column(name = "agrupacion_id", nullable = false)
    private UUID agrupacionId;

    @Column(name = "vecino_email", nullable = false)
    private String vecinoEmail;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    @Column(nullable = false)
    private Integer monto;

    @Column(nullable = false)
    private LocalDate vencimiento;

    @Column(nullable = false)
    private boolean pagada;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    public UUID getId() { return id; }
    public UUID getPeriodoId() { return periodoId; }
    public void setPeriodoId(UUID v) { this.periodoId = v; }
    public UUID getAgrupacionId() { return agrupacionId; }
    public void setAgrupacionId(UUID v) { this.agrupacionId = v; }
    public String getVecinoEmail() { return vecinoEmail; }
    public void setVecinoEmail(String v) { this.vecinoEmail = v; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String v) { this.etiqueta = v; }
    public Integer getMonto() { return monto; }
    public void setMonto(Integer v) { this.monto = v; }
    public LocalDate getVencimiento() { return vencimiento; }
    public void setVencimiento(LocalDate v) { this.vencimiento = v; }
    public boolean isPagada() { return pagada; }
    public void setPagada(boolean v) { this.pagada = v; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime v) { this.fechaPago = v; }
}
