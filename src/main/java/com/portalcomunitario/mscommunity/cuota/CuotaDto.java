package com.portalcomunitario.mscommunity.cuota;

import java.time.LocalDate;
import java.util.UUID;

public record CuotaDto(
        UUID id, String vecinoEmail, String etiqueta, Integer monto,
        String vencimiento, boolean pagada, boolean vencida
) {
    public static CuotaDto from(Cuota c) {
        boolean vencida = !c.isPagada() && c.getVencimiento() != null && c.getVencimiento().isBefore(LocalDate.now());
        return new CuotaDto(c.getId(), c.getVecinoEmail(), c.getEtiqueta(), c.getMonto(),
                c.getVencimiento() != null ? c.getVencimiento().toString() : null,
                c.isPagada(), vencida);
    }
}
