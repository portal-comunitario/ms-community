package com.portalcomunitario.mscommunity.aviso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dominio B — Aviso del Tablón Vecinal (marketplace / clasificados).
 * Lo publica un vecino, nace PENDIENTE y requiere moderación del dirigente.
 * Geolocalizable (aparece en el mapa) y puede marcarse como resuelto/vendido.
 */
@Entity
@Table(name = "avisos_tablon")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AvisoCategoria categoria;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    private Double latitud;

    private Double longitud;

    @Column(length = 500)
    private String direccion;

    /** Precio en pesos chilenos (opcional, para COMPRA_VENTA / ARRIENDO). */
    private Integer precio;

    @Column(length = 255)
    private String contacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvisoEstado estado;

    @Column(nullable = false)
    private boolean resuelto;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (categoria == null) {
            categoria = AvisoCategoria.SERVICIO;
        }
        if (estado == null) {
            estado = AvisoEstado.PENDIENTE;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public AvisoCategoria getCategoria() { return categoria; }
    public void setCategoria(AvisoCategoria categoria) { this.categoria = categoria; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public AvisoEstado getEstado() { return estado; }
    public void setEstado(AvisoEstado estado) { this.estado = estado; }

    public boolean isResuelto() { return resuelto; }
    public void setResuelto(boolean resuelto) { this.resuelto = resuelto; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
