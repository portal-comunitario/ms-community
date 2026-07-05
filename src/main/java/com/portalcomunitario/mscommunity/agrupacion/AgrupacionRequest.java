package com.portalcomunitario.mscommunity.agrupacion;

import java.time.LocalDate;

public record AgrupacionRequest(
        String nombre,
        String descripcion,
        String responsable,
        Integer reunionDiaSemana,
        String reunionHora,
        LocalDate pausaInicio,
        LocalDate pausaFin
) {
}
