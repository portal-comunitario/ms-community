package com.portalcomunitario.mscommunity.messaging;

/** Destinatario con contacto y consentimiento (misma forma que consume ms-notifications). */
public record Destinatario(
        String nombre,
        String email,
        String telefono,
        boolean notificacionesActivas
) {
}
