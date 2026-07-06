package com.portalcomunitario.mscommunity.aviso;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AvisoService {

    private final AvisoRepository repository;

    public AvisoService(AvisoRepository repository) {
        this.repository = repository;
    }

    /** Admins ven todos; vecinos solo APROBADO. */
    public List<AvisoResponse> findAll(String role) {
        if ("COMMUNITY_ADMIN".equals(role) || "PLATFORM_ADMIN".equals(role)) {
            return repository.findAllByOrderByCreatedAtDesc().stream()
                    .map(AvisoResponse::from)
                    .toList();
        }
        return repository.findByEstadoOrderByCreatedAtDesc(AvisoEstado.APROBADO).stream()
                .map(AvisoResponse::from)
                .toList();
    }

    /** Cola de moderación. */
    public List<AvisoResponse> findPendientes() {
        return repository.findByEstadoOrderByCreatedAtDesc(AvisoEstado.PENDIENTE).stream()
                .map(AvisoResponse::from)
                .toList();
    }

    public AvisoResponse findById(UUID id) {
        return AvisoResponse.from(get(id));
    }

    public AvisoResponse create(AvisoRequest req, String authorEmail) {
        Aviso a = new Aviso();
        a.setTitulo(req.titulo());
        a.setAuthorEmail(authorEmail);
        a.setDescripcion(req.descripcion());
        a.setCategoria(parseCategoria(req.categoria()));
        a.setLatitud(req.latitud());
        a.setLongitud(req.longitud());
        a.setDireccion(req.direccion());
        a.setPrecio(req.precio());
        a.setContacto(req.contacto());
        a.setEstado(AvisoEstado.PENDIENTE);
        a.setResuelto(false);
        return AvisoResponse.from(repository.save(a));
    }

    public AvisoResponse aprobar(UUID id) {
        Aviso a = get(id);
        a.setEstado(AvisoEstado.APROBADO);
        return AvisoResponse.from(repository.save(a));
    }

    public AvisoResponse rechazar(UUID id) {
        Aviso a = get(id);
        a.setEstado(AvisoEstado.RECHAZADO);
        return AvisoResponse.from(repository.save(a));
    }

    /** El autor (o un admin) marca su aviso como resuelto/vendido. */
    public AvisoResponse marcarResuelto(UUID id, String requesterEmail, String role) {
        Aviso a = get(id);
        requireAuthorOrAdmin(a, requesterEmail, role);
        a.setResuelto(true);
        return AvisoResponse.from(repository.save(a));
    }

    /** El autor (o un admin) edita su aviso. Conserva estado y autor originales. */
    public AvisoResponse update(UUID id, AvisoRequest req, String requesterEmail, String role) {
        Aviso a = get(id);
        requireAuthorOrAdmin(a, requesterEmail, role);
        a.setTitulo(req.titulo());
        a.setDescripcion(req.descripcion());
        a.setCategoria(parseCategoria(req.categoria()));
        a.setLatitud(req.latitud());
        a.setLongitud(req.longitud());
        a.setDireccion(req.direccion());
        a.setPrecio(req.precio());
        a.setContacto(req.contacto());
        return AvisoResponse.from(repository.save(a));
    }

    public void delete(UUID id, String requesterEmail, String role) {
        Aviso a = get(id);
        requireAuthorOrAdmin(a, requesterEmail, role);
        repository.deleteById(id);
    }

    private void requireAuthorOrAdmin(Aviso a, String requesterEmail, String role) {
        boolean isAdmin = "COMMUNITY_ADMIN".equals(role) || "PLATFORM_ADMIN".equals(role);
        boolean isAuthor = a.getAuthorEmail() != null && a.getAuthorEmail().equalsIgnoreCase(requesterEmail);
        if (!isAdmin && !isAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor o un dirigente puede modificar este aviso");
        }
    }

    private Aviso get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado"));
    }

    private AvisoCategoria parseCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return AvisoCategoria.SERVICIO;
        try {
            return AvisoCategoria.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AvisoCategoria.SERVICIO;
        }
    }
}
