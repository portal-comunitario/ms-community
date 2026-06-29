package com.portalcomunitario.mscommunity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String ubicacion,
        String authorEmail,
        LocalDateTime createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitulo(),
                event.getDescripcion(),
                event.getFechaInicio(),
                event.getFechaFin(),
                event.getUbicacion(),
                event.getAuthorEmail(),
                event.getCreatedAt()
        );
    }
}
