package com.portalcomunitario.mscommunity.post;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Admins y moderadores ven todos los posts.
     * Vecinos solo ven los APROBADO.
     */
    public List<PostResponse> findAll(String role) {
        if ("COMMUNITY_ADMIN".equals(role) || "PLATFORM_ADMIN".equals(role)) {
            return postRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(PostResponse::from)
                    .toList();
        }
        return postRepository.findByEstadoOrderByCreatedAtDesc(PostEstado.APROBADO).stream()
                .map(PostResponse::from)
                .toList();
    }

    /** Cola de moderación — solo posts PENDIENTE. */
    public List<PostResponse> findPendientes() {
        return postRepository.findByEstadoOrderByCreatedAtDesc(PostEstado.PENDIENTE).stream()
                .map(PostResponse::from)
                .toList();
    }

    public PostResponse findById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        return PostResponse.from(post);
    }

    public PostResponse create(PostRequest req, String authorEmail) {
        Post post = new Post();
        post.setTitulo(req.titulo());
        post.setContenido(req.contenido());
        post.setAuthorEmail(authorEmail);
        post.setTipo(parseTipo(req.tipo()));
        post.setLatitud(req.latitud());
        post.setLongitud(req.longitud());
        post.setDireccion(req.direccion());
        post.setEstado(PostEstado.PENDIENTE);
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse aprobar(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        post.setEstado(PostEstado.APROBADO);
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse rechazar(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        post.setEstado(PostEstado.RECHAZADO);
        return PostResponse.from(postRepository.save(post));
    }

    public void delete(UUID id) {
        if (!postRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }
        postRepository.deleteById(id);
    }

    private PostTipo parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return PostTipo.ANUNCIO;
        try {
            return PostTipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PostTipo.ANUNCIO;
        }
    }
}
