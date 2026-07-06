package com.portalcomunitario.mscommunity.aviso;

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
 * Dominio B — Tablón Vecinal. Cualquier vecino publica (queda PENDIENTE);
 * moderación (pendientes/aprobar/rechazar) solo dirigentes; el autor puede
 * marcar resuelto y borrar el suyo.
 */
@RestController
@RequestMapping("/avisos")
public class AvisoController {

    private final AvisoService service;

    public AvisoController(AvisoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AvisoResponse> findAll(Authentication auth) {
        return service.findAll(extractRole(auth));
    }

    @GetMapping("/pendientes")
    public List<AvisoResponse> findPendientes(Authentication auth) {
        requireAdmin(auth);
        return service.findPendientes();
    }

    @GetMapping("/{id}")
    public AvisoResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvisoResponse create(@RequestBody AvisoRequest request, Authentication auth) {
        return service.create(request, extractEmail(auth));
    }

    @PutMapping("/{id}")
    public AvisoResponse update(@PathVariable UUID id, @RequestBody AvisoRequest request, Authentication auth) {
        return service.update(id, request, extractEmail(auth), extractRole(auth));
    }

    @PutMapping("/{id}/aprobar")
    public AvisoResponse aprobar(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return service.aprobar(id);
    }

    @PutMapping("/{id}/rechazar")
    public AvisoResponse rechazar(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return service.rechazar(id);
    }

    @PutMapping("/{id}/resuelto")
    public AvisoResponse marcarResuelto(@PathVariable UUID id, Authentication auth) {
        return service.marcarResuelto(id, extractEmail(auth), extractRole(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, extractEmail(auth), extractRole(auth));
    }

    private String extractRole(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String role = jwtAuth.getToken().getClaimAsString("role");
            return role != null ? role : "VECINO";
        }
        return "VECINO";
    }

    /** Email del autor: subject del JWT, con respaldo en los claims email/name. */
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }
    }
}
