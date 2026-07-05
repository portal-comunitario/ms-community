package com.portalcomunitario.mscommunity.comunicado;

public record ComunicadoRequest(
        String titulo,
        String contenido,
        String categoria,
        String imagenUrl
) {
}
