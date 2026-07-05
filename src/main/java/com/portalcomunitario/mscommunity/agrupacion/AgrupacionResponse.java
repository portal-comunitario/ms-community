package com.portalcomunitario.mscommunity.agrupacion;

import java.util.List;
import java.util.UUID;

public record AgrupacionResponse(
        UUID id,
        String nombre,
        String descripcion,
        String responsable,
        boolean manejaCuotas,
        long socios,
        boolean inscrito,
        Integer reunionDiaSemana,
        String reunionHora,
        String pausaInicio,
        String pausaFin,
        List<String> reunionesCanceladas
) {
}
