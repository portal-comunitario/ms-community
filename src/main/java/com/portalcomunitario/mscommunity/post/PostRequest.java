package com.portalcomunitario.mscommunity.post;

public record PostRequest(
        String titulo,
        String contenido,
        String tipo,
        Double latitud,
        Double longitud,
        String direccion
) {
}
