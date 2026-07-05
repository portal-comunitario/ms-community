package com.portalcomunitario.mscommunity.agrupacion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgrupacionService {

    private final AgrupacionRepository repository;
    private final InscripcionAgrupacionRepository inscripcionRepository;
    private final ReunionCanceladaRepository reunionCanceladaRepository;

    public AgrupacionService(AgrupacionRepository repository,
                             InscripcionAgrupacionRepository inscripcionRepository,
                             ReunionCanceladaRepository reunionCanceladaRepository) {
        this.repository = repository;
        this.inscripcionRepository = inscripcionRepository;
        this.reunionCanceladaRepository = reunionCanceladaRepository;
    }

    public List<AgrupacionResponse> findAll(String requesterEmail) {
        Set<UUID> misInscripciones = inscripcionRepository.findByVecinoEmail(requesterEmail).stream()
                .map(InscripcionAgrupacion::getAgrupacionId)
                .collect(Collectors.toSet());
        return repository.findAllByOrderByNombreAsc().stream()
                .map(a -> toResponse(a, misInscripciones.contains(a.getId())))
                .toList();
    }

    public AgrupacionResponse create(AgrupacionRequest req) {
        Agrupacion a = new Agrupacion();
        aplicar(a, req);
        return toResponse(repository.save(a), false);
    }

    public AgrupacionResponse update(UUID id, AgrupacionRequest req, String requesterEmail) {
        Agrupacion a = get(id);
        aplicar(a, req);
        boolean inscrito = inscripcionRepository.existsByAgrupacionIdAndVecinoEmail(id, requesterEmail);
        return toResponse(repository.save(a), inscrito);
    }

    private void aplicar(Agrupacion a, AgrupacionRequest req) {
        a.setNombre(req.nombre());
        a.setDescripcion(req.descripcion());
        a.setResponsable(req.responsable());
        a.setReunionDiaSemana(normalizeDia(req.reunionDiaSemana()));
        a.setReunionHora(parseHora(req.reunionHora()));
        a.setPausaInicio(req.pausaInicio());
        a.setPausaFin(req.pausaFin());
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agrupación no encontrada");
        }
        repository.deleteById(id);
    }

    @Transactional
    public void inscribirse(UUID id, String email) {
        get(id);
        if (inscripcionRepository.existsByAgrupacionIdAndVecinoEmail(id, email)) return;
        InscripcionAgrupacion i = new InscripcionAgrupacion();
        i.setAgrupacionId(id);
        i.setVecinoEmail(email);
        inscripcionRepository.save(i);
    }

    @Transactional
    public void salir(UUID id, String email) {
        inscripcionRepository.deleteByAgrupacionIdAndVecinoEmail(id, email);
    }

    public List<UUID> misInscripciones(String email) {
        return inscripcionRepository.findByVecinoEmail(email).stream()
                .map(InscripcionAgrupacion::getAgrupacionId)
                .toList();
    }

    public List<String> socios(UUID id) {
        return inscripcionRepository.findByAgrupacionId(id).stream()
                .map(InscripcionAgrupacion::getVecinoEmail)
                .toList();
    }

    public void cancelarReunion(UUID id, LocalDate fecha) {
        get(id);
        if (fecha == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha requerida");
        if (reunionCanceladaRepository.existsByAgrupacionIdAndFecha(id, fecha)) return;
        ReunionCancelada rc = new ReunionCancelada();
        rc.setAgrupacionId(id);
        rc.setFecha(fecha);
        reunionCanceladaRepository.save(rc);
    }

    @Transactional
    public void reactivarReunion(UUID id, LocalDate fecha) {
        reunionCanceladaRepository.deleteByAgrupacionIdAndFecha(id, fecha);
    }

    private Agrupacion get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agrupación no encontrada"));
    }

    private AgrupacionResponse toResponse(Agrupacion a, boolean inscrito) {
        List<String> canceladas = reunionCanceladaRepository.findByAgrupacionId(a.getId()).stream()
                .map(rc -> rc.getFecha().toString())
                .toList();
        return new AgrupacionResponse(
                a.getId(), a.getNombre(), a.getDescripcion(), a.getResponsable(),
                a.isManejaCuotas(), inscripcionRepository.countByAgrupacionId(a.getId()), inscrito,
                a.getReunionDiaSemana(),
                a.getReunionHora() != null ? a.getReunionHora().toString() : null,
                a.getPausaInicio() != null ? a.getPausaInicio().toString() : null,
                a.getPausaFin() != null ? a.getPausaFin().toString() : null,
                canceladas);
    }

    private Integer normalizeDia(Integer dia) {
        if (dia == null || dia < 1 || dia > 7) return null;
        return dia;
    }

    private LocalTime parseHora(String hora) {
        if (hora == null || hora.isBlank()) return null;
        try {
            return LocalTime.parse(hora.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
