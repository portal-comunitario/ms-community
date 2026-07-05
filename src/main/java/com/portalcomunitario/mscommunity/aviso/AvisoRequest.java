package com.portalcomunitario.mscommunity.aviso;

public record AvisoRequest(
        String titulo,
        String descripcion,
        String categoria,
        Double latitud,
        Double longitud,
        String direccion,
        Integer precio,
        String contacto
) {
}
