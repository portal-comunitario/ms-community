package com.portalcomunitario.mscommunity.cuota;

import java.time.LocalDate;

public record CuotaRequest(
        Integer monto, String periodicidad, LocalDate fechaInicio, LocalDate fechaFin
) {
}
