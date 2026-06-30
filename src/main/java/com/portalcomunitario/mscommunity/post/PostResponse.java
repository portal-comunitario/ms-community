package com.portalcomunitario.mscommunity.post;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String titulo,
        String contenido,
        String authorEmail,
        String tipo,
        LocalDateTime createdAt,
        Double latitud,
        Double longitud,
        String direccion
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitulo(),
                post.getContenido(),
                post.getAuthorEmail(),
                post.getTipo() != null ? post.getTipo().name() : null,
                post.getCreatedAt(),
                post.getLatitud(),
                post.getLongitud(),
                post.getDireccion()
        );
    }
}
