package com.portalcomunitario.mscommunity.asistencia;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class AsistenciaController {

    private final AsistenciaService service;

    public AsistenciaController(AsistenciaService service) {
        this.service = service;
    }

    /** Lista para pasar lista (dirigente): socios de la actividad con su estado. */
    @GetMapping("/eventos/{eventoId}/asistencia")
    public List<AsistenciaSocioDto> deActividad(@PathVariable UUID eventoId, Authentication auth) {
        requireAdmin(auth);
        return service.deActividad(eventoId);
    }

    /** Guarda la asistencia (dirigente): body { presentes: [emails] }. */
    @PutMapping("/eventos/{eventoId}/asistencia")
    public List<AsistenciaSocioDto> marcar(@PathVariable UUID eventoId,
                                           @RequestBody MarcarAsistenciaRequest req,
                                           Authentication auth) {
        requireAdmin(auth);
        service.marcar(eventoId, req != null ? req.presentes() : List.of());
        return service.deActividad(eventoId);
    }

    /** Sesiones de asistencia = ocurrencias de la reunión periódica (autenticado). */
    @GetMapping("/agrupaciones/{agrupacionId}/asistencia/sesiones")
    public List<SesionAsistenciaDto> sesiones(@PathVariable UUID agrupacionId) {
        return service.sesiones(agrupacionId);
    }

    /** Pasar lista de una sesión (dirigente): socios con su estado. */
    @GetMapping("/agrupaciones/{agrupacionId}/asistencia/sesiones/{sesionId}")
    public List<AsistenciaSocioDto> deSesion(@PathVariable UUID agrupacionId,
                                             @PathVariable UUID sesionId, Authentication auth) {
        requireAdmin(auth);
        return service.deSesion(agrupacionId, sesionId);
    }

    /** Guarda la asistencia de una sesión (dirigente): body { presentes: [emails] }. */
    @PutMapping("/agrupaciones/{agrupacionId}/asistencia/sesiones/{sesionId}")
    public List<AsistenciaSocioDto> marcarSesion(@PathVariable UUID agrupacionId,
                                                 @PathVariable UUID sesionId,
                                                 @RequestBody MarcarAsistenciaRequest req,
                                                 Authentication auth) {
        requireAdmin(auth);
        service.marcarSesion(agrupacionId, sesionId, req != null ? req.presentes() : List.of());
        return service.deSesion(agrupacionId, sesionId);
    }

    /** Asistencia del vecino autenticado en una agrupación. */
    @GetMapping("/agrupaciones/{id}/mi-asistencia")
    public List<MiAsistenciaDto> miAsistencia(@PathVariable UUID id, Authentication auth) {
        return service.miAsistencia(id, extractEmail(auth));
    }

    public record MarcarAsistenciaRequest(List<String> presentes) {}

    private String extractRole(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String r = jwtAuth.getToken().getClaimAsString("role");
            return r != null ? r : "VECINO";
        }
        return "VECINO";
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            for (String v : new String[]{jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name")}) {
                if (v != null && !v.isBlank()) return v;
            }
        }
        String name = auth != null ? auth.getName() : null;
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        return name;
    }

    private void requireAdmin(Authentication auth) {
        String r = extractRole(auth);
        if (!"COMMUNITY_ADMIN".equals(r) && !"PLATFORM_ADMIN".equals(r)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los dirigentes pueden gestionar la asistencia");
        }
    }
}
