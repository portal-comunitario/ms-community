package com.portalcomunitario.mscommunity.comunicado;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Dominio A — Comunicados. Lectura pública (autenticada); creación/edición/borrado
 * solo para dirigentes (COMMUNITY_ADMIN / PLATFORM_ADMIN). Sin moderación.
 */
@RestController
@RequestMapping("/comunicados")
public class ComunicadoController {

    private final ComunicadoService service;

    public ComunicadoController(ComunicadoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ComunicadoResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ComunicadoResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComunicadoResponse create(@RequestBody ComunicadoRequest request, Authentication auth) {
        requireAdmin(auth);
        return service.create(request, extractEmail(auth));
    }

    @PutMapping("/{id}")
    public ComunicadoResponse update(@PathVariable UUID id, @RequestBody ComunicadoRequest request, Authentication auth) {
        requireAdmin(auth);
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        service.delete(id);
    }

    private String extractRole(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String role = jwtAuth.getToken().getClaimAsString("role");
            return role != null ? role : "VECINO";
        }
        return "VECINO";
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String email = firstNonBlank(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
            if (email != null) return email;
        }
        String name = auth != null ? auth.getName() : null;
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo determinar el usuario autenticado");
        }
        return name;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private void requireAdmin(Authentication auth) {
        String role = extractRole(auth);
        if (!"COMMUNITY_ADMIN".equals(role) && !"PLATFORM_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los dirigentes pueden publicar comunicados");
        }
    }
}
