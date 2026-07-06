package com.portalcomunitario.mscommunity.asistencia;

import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacionRepository;
import com.portalcomunitario.mscommunity.event.Event;
import com.portalcomunitario.mscommunity.event.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Asistencia a las actividades de una agrupación.
 * El dirigente "pasa lista" por actividad (evento asociado a una agrupación);
 * cada socio queda marcado presente o ausente. El vecino ve su propia asistencia.
 */
@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepo;
    private final EventRepository eventRepo;
    private final InscripcionAgrupacionRepository inscripcionRepo;

    public AsistenciaService(AsistenciaRepository asistenciaRepo,
                             EventRepository eventRepo,
                             InscripcionAgrupacionRepository inscripcionRepo) {
        this.asistenciaRepo = asistenciaRepo;
        this.eventRepo = eventRepo;
        this.inscripcionRepo = inscripcionRepo;
    }

    private Event actividadDeAgrupacion(UUID eventoId) {
        Event ev = eventRepo.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
        if (ev.getAgrupacionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La asistencia solo aplica a actividades de una agrupación");
        }
        return ev;
    }

    private List<String> sociosDe(UUID agrupacionId) {
        return inscripcionRepo.findByAgrupacionId(agrupacionId).stream()
                .map(i -> i.getVecinoEmail())
                .toList();
    }

    /** Lista para "pasar lista": cada socio con su estado actual (ausente por defecto). */
    public List<AsistenciaSocioDto> deActividad(UUID eventoId) {
        Event ev = actividadDeAgrupacion(eventoId);
        Map<String, Boolean> registrado = new LinkedHashMap<>();
        for (Asistencia a : asistenciaRepo.findByEventoId(eventoId)) {
            registrado.put(a.getVecinoEmail(), a.isPresente());
        }
        return sociosDe(ev.getAgrupacionId()).stream()
                .map(email -> new AsistenciaSocioDto(email, registrado.getOrDefault(email, false)))
                .toList();
    }

    /** Guarda la lista: los correos en {@code presentes} quedan presente=true; el resto de socios, ausente. */
    @Transactional
    public void marcar(UUID eventoId, List<String> presentes) {
        Event ev = actividadDeAgrupacion(eventoId);
        List<String> quienes = presentes != null ? presentes : List.of();
        for (String email : sociosDe(ev.getAgrupacionId())) {
            boolean presente = quienes.contains(email);
            Asistencia a = asistenciaRepo.findByEventoIdAndVecinoEmail(eventoId, email)
                    .orElseGet(Asistencia::new);
            a.setEventoId(eventoId);
            a.setAgrupacionId(ev.getAgrupacionId());
            a.setVecinoEmail(email);
            a.setPresente(presente);
            asistenciaRepo.save(a);
        }
    }

    /** Asistencia del propio vecino en una agrupación. */
    public List<MiAsistenciaDto> miAsistencia(UUID agrupacionId, String email) {
        return asistenciaRepo.findByAgrupacionIdAndVecinoEmail(agrupacionId, email).stream()
                .map(a -> new MiAsistenciaDto(a.getEventoId(), a.isPresente()))
                .toList();
    }
}
