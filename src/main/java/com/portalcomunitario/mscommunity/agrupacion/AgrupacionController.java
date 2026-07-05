package com.portalcomunitario.mscommunity.agrupacion;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrupaciones comunitarias. Lectura autenticada; crear/editar/borrar solo dirigentes;
 * inscribirse/salir cualquier vecino.
 */
@RestController
@RequestMapping("/agrupaciones")
public class AgrupacionController {

    private final AgrupacionService service;

    public AgrupacionController(AgrupacionService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgrupacionResponse> findAll(Authentication auth) {
        return service.findAll(extractEmail(auth));
    }

    @GetMapping("/mis-inscripciones")
    public List<UUID> misInscripciones(Authentication auth) {
        return service.misInscripciones(extractEmail(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgrupacionResponse create(@RequestBody AgrupacionRequest request, Authentication auth) {
        requireAdmin(auth);
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AgrupacionResponse update(@PathVariable UUID id, @RequestBody AgrupacionRequest request, Authentication auth) {
        requireAdmin(auth);
        return service.update(id, request, extractEmail(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        service.delete(id);
    }

    @PostMapping("/{id}/inscribirse")
    public void inscribirse(@PathVariable UUID id, Authentication auth) {
        service.inscribirse(id, extractEmail(auth));
    }

    @DeleteMapping("/{id}/inscribirse")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@PathVariable UUID id, Authentication auth) {
        service.salir(id, extractEmail(auth));
    }

    @GetMapping("/{id}/socios")
    public List<String> socios(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return service.socios(id);
    }

    @PostMapping("/{id}/reuniones/cancelar")
    public void cancelarReunion(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        requireAdmin(auth);
        String fecha = body.get("fecha");
        if (fecha == null || fecha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha requerida (yyyy-MM-dd)");
        }
        service.cancelarReunion(id, LocalDate.parse(fecha));
    }

    @DeleteMapping("/{id}/reuniones/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivarReunion(@PathVariable UUID id, @RequestParam String fecha, Authentication auth) {
        requireAdmin(auth);
        service.reactivarReunion(id, LocalDate.parse(fecha));
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los dirigentes pueden gestionar agrupaciones");
        }
    }
}
