package com.portalcomunitario.mscommunity.event;

import com.portalcomunitario.mscommunity.contacto.ContactoClient;
import com.portalcomunitario.mscommunity.messaging.Destinatario;
import com.portalcomunitario.mscommunity.messaging.NotificacionEvento;
import com.portalcomunitario.mscommunity.messaging.NotificacionPublisher;
import com.portalcomunitario.mscommunity.messaging.RabbitConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EventRepository eventRepository;
    private final ContactoClient contactoClient;
    private final NotificacionPublisher publisher;

    public EventService(EventRepository eventRepository,
                        ContactoClient contactoClient,
                        NotificacionPublisher publisher) {
        this.eventRepository = eventRepository;
        this.contactoClient = contactoClient;
        this.publisher = publisher;
    }

    public List<EventResponse> findAll() {
        return eventRepository.findAllByOrderByFechaInicioAsc().stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse findById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        return EventResponse.from(event);
    }

    /** El dirigente difunde un evento de comunidad a todos los vecinos (broadcast). */
    public EventResponse notificarComunidad(UUID id) {
        Event ev = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        if (ev.getAgrupacionId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo los eventos de comunidad se pueden difundir (los de agrupación son internos)");
        }
        publicar(ev, RabbitConfig.RK_EVENTO_COMUNIDAD, false);
        ev.setNotificadoComunidad(LocalDateTime.now());
        eventRepository.save(ev);
        return EventResponse.from(ev);
    }

    /** Usado por el scheduler para el recordatorio del día antes. */
    public void publicarRecordatorio(Event ev) {
        publicar(ev, RabbitConfig.RK_EVENTO_RECORDATORIO, true);
    }

    private void publicar(Event ev, String routingKey, boolean recordatorio) {
        List<Destinatario> destinatarios = contactoClient.todos().stream()
                .map(c -> new Destinatario(c.nombre(), c.email(), c.telefono(), c.notificacionesActivas()))
                .toList();
        String cuando = ev.getFechaInicio() != null ? ev.getFechaInicio().format(FECHA) : "";
        String lugar = ev.getUbicacion() != null && !ev.getUbicacion().isBlank() ? " en " + ev.getUbicacion() : "";
        String titulo = recordatorio ? "Recordatorio: " + ev.getTitulo() : ev.getTitulo();
        String mensaje = (recordatorio ? "Mañana: " : "Te invitamos a: ") + ev.getTitulo()
                + " (" + cuando + ")" + lugar + "."
                + (ev.getDescripcion() != null && !ev.getDescripcion().isBlank() ? " " + ev.getDescripcion() : "");
        NotificacionEvento evento = new NotificacionEvento(
                recordatorio ? "EVENTO_RECORDATORIO" : "EVENTO_COMUNIDAD", titulo, mensaje, destinatarios);
        publisher.publicar(routingKey, evento);
    }

    public EventResponse create(EventRequest req, String authorEmail, String authorNombre) {
        Event event = new Event();
        event.setTitulo(req.titulo());
        event.setDescripcion(req.descripcion());
        event.setFechaInicio(req.fechaInicio());
        event.setFechaFin(req.fechaFin());
        event.setUbicacion(req.ubicacion());
        event.setCategoria(parseCategoria(req.categoria()));
        event.setSubcategoria(blankToNull(req.subcategoria()));
        event.setColor(normalizaColor(req.color()));
        event.setAgrupacionId(req.agrupacionId());
        event.setLatitud(req.latitud());
        event.setLongitud(req.longitud());
        event.setRecurrente(req.recurrente());
        if (req.recurrente()) {
            event.setFrecuencia(normalizaFrecuencia(req.frecuencia()));
            event.setIntervalo(req.intervalo() != null && req.intervalo() > 0 ? req.intervalo() : 1);
            event.setRecurrenciaFin(req.recurrenciaFin());
        }
        event.setAuthorEmail(authorEmail);
        event.setAuthorNombre(authorNombre);
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse update(UUID id, EventRequest req) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        event.setTitulo(req.titulo());
        event.setDescripcion(req.descripcion());
        event.setFechaInicio(req.fechaInicio());
        event.setFechaFin(req.fechaFin());
        event.setUbicacion(req.ubicacion());
        event.setCategoria(parseCategoria(req.categoria()));
        event.setSubcategoria(blankToNull(req.subcategoria()));
        event.setColor(normalizaColor(req.color()));
        event.setAgrupacionId(req.agrupacionId());
        event.setLatitud(req.latitud());
        event.setLongitud(req.longitud());
        event.setRecurrente(req.recurrente());
        if (req.recurrente()) {
            event.setFrecuencia(normalizaFrecuencia(req.frecuencia()));
            event.setIntervalo(req.intervalo() != null && req.intervalo() > 0 ? req.intervalo() : 1);
            event.setRecurrenciaFin(req.recurrenciaFin());
        } else {
            event.setFrecuencia(null);
            event.setIntervalo(null);
            event.setRecurrenciaFin(null);
        }
        return EventResponse.from(eventRepository.save(event));
    }

    public void delete(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
        eventRepository.deleteById(id);
    }

    private String normalizaFrecuencia(String f) {
        if (f == null || f.isBlank()) return "SEMANAL";
        String v = f.trim().toUpperCase();
        return switch (v) {
            case "DIARIA", "SEMANAL", "MENSUAL", "ANUAL" -> v;
            default -> "SEMANAL";
        };
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private String normalizaColor(String c) {
        if (c == null || c.isBlank()) return null;
        String v = c.trim();
        return v.matches("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})") ? v : null;
    }

    private EventCategoria parseCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return EventCategoria.GENERAL;
        try {
            return EventCategoria.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EventCategoria.GENERAL;
        }
    }
}
