package com.portalcomunitario.mscommunity.aviso;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvisoResponse(
        UUID id,
        String titulo,
        String descripcion,
        String categoria,
        String authorEmail,
        Double latitud,
        Double longitud,
        String direccion,
        Integer precio,
        String contacto,
        String estado,
        boolean resuelto,
        LocalDateTime resueltoAt,
        LocalDateTime createdAt
) {
    public static AvisoResponse from(Aviso a) {
        return new AvisoResponse(
                a.getId(),
                a.getTitulo(),
                a.getDescripcion(),
                a.getCategoria() != null ? a.getCategoria().name() : null,
                a.getAuthorEmail(),
                a.getLatitud(),
                a.getLongitud(),
                a.getDireccion(),
                a.getPrecio(),
                a.getContacto(),
                a.getEstado() != null ? a.getEstado().name() : AvisoEstado.PENDIENTE.name(),
                a.isResuelto(),
                a.getResueltoAt(),
                a.getCreatedAt()
        );
    }
}
