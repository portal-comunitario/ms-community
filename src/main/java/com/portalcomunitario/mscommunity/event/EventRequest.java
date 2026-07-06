package com.portalcomunitario.mscommunity.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
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
        LocalDate recurrenciaFin
) {
}
