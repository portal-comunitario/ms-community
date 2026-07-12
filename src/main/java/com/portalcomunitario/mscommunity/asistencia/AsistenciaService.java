package com.portalcomunitario.mscommunity.asistencia;

import com.portalcomunitario.mscommunity.agrupacion.Agrupacion;
import com.portalcomunitario.mscommunity.agrupacion.AgrupacionRepository;
import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacionRepository;
import com.portalcomunitario.mscommunity.agrupacion.ReunionCancelada;
import com.portalcomunitario.mscommunity.agrupacion.ReunionCanceladaRepository;
import com.portalcomunitario.mscommunity.event.Event;
import com.portalcomunitario.mscommunity.event.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Asistencia a las agrupaciones. Se pasa lista por SESIÓN: cada sesión es una
 * ocurrencia de la reunión periódica (ej. cada viernes), no un evento aparte.
 * Cada socio queda presente o ausente; el vecino ve su propia asistencia.
 */
@Service
public class AsistenciaService {

    /** Cuántas semanas hacia atrás se listan reuniones para pasar lista. */
    private static final int SEMANAS_ATRAS = 12;
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AsistenciaRepository asistenciaRepo;
    private final EventRepository eventRepo;
    private final InscripcionAgrupacionRepository inscripcionRepo;
    private final AgrupacionRepository agrupacionRepo;
    private final ReunionCanceladaRepository reunionCanceladaRepo;

    public AsistenciaService(AsistenciaRepository asistenciaRepo,
                             EventRepository eventRepo,
                             InscripcionAgrupacionRepository inscripcionRepo,
                             AgrupacionRepository agrupacionRepo,
                             ReunionCanceladaRepository reunionCanceladaRepo) {
        this.asistenciaRepo = asistenciaRepo;
        this.eventRepo = eventRepo;
        this.inscripcionRepo = inscripcionRepo;
        this.agrupacionRepo = agrupacionRepo;
        this.reunionCanceladaRepo = reunionCanceladaRepo;
    }

    // ---- Sesiones derivadas de la reunión periódica ----

    /**
     * Ocurrencias de la reunión periódica (últimas {@value #SEMANAS_ATRAS} semanas hasta hoy),
     * excluyendo pausa y reuniones canceladas. Cada una es una "sesión" para pasar lista,
     * con un id determinístico y estable por (agrupación, fecha).
     */
    public List<SesionAsistenciaDto> sesiones(UUID agrupacionId) {
        Agrupacion a = agrupacionRepo.findById(agrupacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agrupación no encontrada"));
        Integer dow = a.getReunionDiaSemana();
        if (dow == null) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusWeeks(SEMANAS_ATRAS)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dow)));
        Set<LocalDate> canceladas = reunionCanceladaRepo.findByAgrupacionId(agrupacionId).stream()
                .map(ReunionCancelada::getFecha)
                .collect(Collectors.toSet());
        String hora = a.getReunionHora() != null ? a.getReunionHora().format(HORA_FMT) : "";

        List<SesionAsistenciaDto> out = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hoy); d = d.plusWeeks(1)) {
            if (enPausa(a, d) || canceladas.contains(d)) {
                continue;
            }
            out.add(new SesionAsistenciaDto(sesionId(agrupacionId, d), d, hora));
        }
        Collections.reverse(out); // más reciente primero
        return out;
    }

    private boolean enPausa(Agrupacion a, LocalDate d) {
        if (a.getPausaInicio() == null || d.isBefore(a.getPausaInicio())) {
            return false;
        }
        return a.getPausaFin() == null || !d.isAfter(a.getPausaFin());
    }

    /** Id determinístico y estable de una sesión por (agrupación, fecha). */
    static UUID sesionId(UUID agrupacionId, LocalDate fecha) {
        return UUID.nameUUIDFromBytes((agrupacionId + ":" + fecha).getBytes(StandardCharsets.UTF_8));
    }

    /** Lista para pasar lista en una sesión de reunión: cada socio con su estado. */
    public List<AsistenciaSocioDto> deSesion(UUID agrupacionId, UUID sesionId) {
        Map<String, Boolean> registrado = new LinkedHashMap<>();
        for (Asistencia a : asistenciaRepo.findByEventoId(sesionId)) {
            registrado.put(a.getVecinoEmail(), a.isPresente());
        }
        return sociosDe(agrupacionId).stream()
                .map(email -> new AsistenciaSocioDto(email, registrado.getOrDefault(email, false)))
                .toList();
    }

    /** Guarda la lista de una sesión de reunión. */
    @Transactional
    public void marcarSesion(UUID agrupacionId, UUID sesionId, List<String> presentes) {
        List<String> quienes = presentes != null ? presentes : List.of();
        for (String email : sociosDe(agrupacionId)) {
            boolean presente = quienes.contains(email);
            Asistencia a = asistenciaRepo.findByEventoIdAndVecinoEmail(sesionId, email)
                    .orElseGet(Asistencia::new);
            a.setEventoId(sesionId);
            a.setAgrupacionId(agrupacionId);
            a.setVecinoEmail(email);
            a.setPresente(presente);
            asistenciaRepo.save(a);
        }
    }

    // ---- Asistencia por actividad (evento) — se mantiene para actividades puntuales ----

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

    /** Guarda la lista: los correos en {@code presentes} quedan presente=true; el resto, ausente. */
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

    /** Asistencia del propio vecino en una agrupación (por sesión o actividad). */
    public List<MiAsistenciaDto> miAsistencia(UUID agrupacionId, String email) {
        return asistenciaRepo.findByAgrupacionIdAndVecinoEmail(agrupacionId, email).stream()
                .map(a -> new MiAsistenciaDto(a.getEventoId(), a.isPresente()))
                .toList();
    }
}
