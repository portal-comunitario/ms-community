package com.portalcomunitario.mscommunity.comunicado;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComunicadoResponse(
        UUID id,
        String titulo,
        String contenido,
        String categoria,
        String imagenUrl,
        String authorEmail,
        LocalDateTime createdAt
) {
    public static ComunicadoResponse from(Comunicado c) {
        return new ComunicadoResponse(
                c.getId(),
                c.getTitulo(),
                c.getContenido(),
                c.getCategoria() != null ? c.getCategoria().name() : ComunicadoCategoria.NOTICIA.name(),
                c.getImagenUrl(),
                c.getAuthorEmail(),
                c.getCreatedAt()
        );
    }
}
