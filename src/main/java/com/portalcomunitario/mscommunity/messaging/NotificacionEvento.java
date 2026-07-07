package com.portalcomunitario.mscommunity.messaging;

import java.util.List;

/** Envelope de evento de notificación (misma forma que consume ms-notifications). */
public record NotificacionEvento(
        String tipo,
        String titulo,
        String mensaje,
        List<Destinatario> destinatarios
) {
}
