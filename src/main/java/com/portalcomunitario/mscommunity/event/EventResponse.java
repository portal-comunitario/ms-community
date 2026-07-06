package com.portalcomunitario.mscommunity.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String ubicacion,
        String categoria,
        String subcategoria,
        String color,
        UUID agrupacionId,
        Double latitud,
        Double longitud,
        boolean recurrente,
        String frecuencia,
        Integer intervalo,
        LocalDate recurrenciaFin,
        String authorEmail,
        String authorNombre,
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
                event.getCategoria() != null ? event.getCategoria().name() : EventCategoria.GENERAL.name(),
                event.getSubcategoria(),
                event.getColor(),
                event.getAgrupacionId(),
                event.getLatitud(),
                event.getLongitud(),
                event.isRecurrente(),
                event.getFrecuencia(),
                event.getIntervalo(),
                event.getRecurrenciaFin(),
                event.getAuthorEmail(),
                event.getAuthorNombre(),
                event.getCreatedAt()
        );
    }
}
