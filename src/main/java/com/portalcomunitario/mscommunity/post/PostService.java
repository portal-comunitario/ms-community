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

    public List<PostResponse> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
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
        return PostResponse.from(postRepository.save(post));
    }

    public void delete(UUID id) {
        if (!postRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }
        postRepository.deleteById(id);
    }

    private PostTipo parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return PostTipo.ANUNCIO;
        }
        try {
            return PostTipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PostTipo.ANUNCIO;
        }
    }
}
