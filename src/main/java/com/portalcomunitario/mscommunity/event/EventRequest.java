package com.portalcomunitario.mscommunity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String ubicacion,
        String categoria,
        UUID agrupacionId
) {
}
