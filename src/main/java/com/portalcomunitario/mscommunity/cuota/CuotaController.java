package com.portalcomunitario.mscommunity.cuota;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class CuotaController {

    private final CuotaService service;

    public CuotaController(CuotaService service) {
        this.service = service;
    }

    @PostMapping("/agrupaciones/{id}/cuotas/activar")
    public CuotaPeriodoDto activar(@PathVariable UUID id, @RequestBody CuotaRequest req, Authentication auth) {
        requireAdmin(auth);
        return CuotaPeriodoDto.from(service.activar(id, req));
    }

    @PutMapping("/agrupaciones/{id}/cuotas/cerrar")
    public CuotaPeriodoDto cerrar(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return CuotaPeriodoDto.from(service.cerrar(id));
    }

    @PutMapping("/agrupaciones/{id}/cuotas/monto")
    public CuotaPeriodoDto actualizarMonto(@PathVariable UUID id, @RequestBody MontoRequest req, Authentication auth) {
        requireAdmin(auth);
        return CuotaPeriodoDto.from(service.actualizarMonto(id, req != null ? req.monto() : null));
    }

    public record MontoRequest(Integer monto) {}

    @GetMapping("/agrupaciones/{id}/cuotas/periodo")
    public CuotaPeriodoDto periodo(@PathVariable UUID id) {
        CuotaPeriodo p = service.periodoActivo(id);
        return p != null ? CuotaPeriodoDto.from(p) : null;
    }

    @GetMapping("/agrupaciones/{id}/cuotas")
    public List<CuotaDto> todas(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return service.deAgrupacion(id).stream().map(CuotaDto::from).toList();
    }

    @GetMapping("/agrupaciones/{id}/cuotas/mias")
    public List<CuotaDto> mias(@PathVariable UUID id, Authentication auth) {
        return service.misCuotas(id, extractEmail(auth)).stream().map(CuotaDto::from).toList();
    }

    @PutMapping("/cuotas/{cuotaId}/pagar")
    public CuotaDto pagar(@PathVariable UUID cuotaId, Authentication auth) {
        requireAdmin(auth);
        return CuotaDto.from(service.marcarPago(cuotaId, true));
    }

    @PutMapping("/cuotas/{cuotaId}/pendiente")
    public CuotaDto pendiente(@PathVariable UUID cuotaId, Authentication auth) {
        requireAdmin(auth);
        return CuotaDto.from(service.marcarPago(cuotaId, false));
    }

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los dirigentes pueden gestionar cuotas");
        }
    }
}
