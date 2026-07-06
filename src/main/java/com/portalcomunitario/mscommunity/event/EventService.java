package com.portalcomunitario.mscommunity.event;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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
