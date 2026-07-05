package com.portalcomunitario.mscommunity.comunicado;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ComunicadoService {

    private final ComunicadoRepository repository;

    public ComunicadoService(ComunicadoRepository repository) {
        this.repository = repository;
    }

    public List<ComunicadoResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(ComunicadoResponse::from)
                .toList();
    }

    public ComunicadoResponse findById(UUID id) {
        return ComunicadoResponse.from(get(id));
    }

    public ComunicadoResponse create(ComunicadoRequest req, String authorEmail) {
        Comunicado c = new Comunicado();
        c.setTitulo(req.titulo());
        c.setContenido(req.contenido());
        c.setCategoria(parseCategoria(req.categoria()));
        c.setImagenUrl(req.imagenUrl());
        c.setAuthorEmail(authorEmail);
        return ComunicadoResponse.from(repository.save(c));
    }

    public ComunicadoResponse update(UUID id, ComunicadoRequest req) {
        Comunicado c = get(id);
        c.setTitulo(req.titulo());
        c.setContenido(req.contenido());
        c.setCategoria(parseCategoria(req.categoria()));
        c.setImagenUrl(req.imagenUrl());
        return ComunicadoResponse.from(repository.save(c));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado no encontrado");
        }
        repository.deleteById(id);
    }

    private Comunicado get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado no encontrado"));
    }

    private ComunicadoCategoria parseCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return ComunicadoCategoria.NOTICIA;
        try {
            return ComunicadoCategoria.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ComunicadoCategoria.NOTICIA;
        }
    }
}
