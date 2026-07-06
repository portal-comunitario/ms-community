package com.portalcomunitario.mscommunity.cuota;

import java.util.UUID;

public record CuotaPeriodoDto(
        UUID id, Integer monto, String periodicidad,
        String fechaInicio, String fechaFin, String estado
) {
    public static CuotaPeriodoDto from(CuotaPeriodo p) {
        return new CuotaPeriodoDto(p.getId(), p.getMonto(),
                p.getPeriodicidad() != null ? p.getPeriodicidad().name() : null,
                p.getFechaInicio() != null ? p.getFechaInicio().toString() : null,
                p.getFechaFin() != null ? p.getFechaFin().toString() : null,
                p.getEstado() != null ? p.getEstado().name() : null);
    }
}
