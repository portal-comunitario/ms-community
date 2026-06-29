package com.portalcomunitario.mscommunity.event;

import java.time.LocalDateTime;

public record EventRequest(
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String ubicacion
) {
}
