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

    public EventResponse create(EventRequest req, String authorEmail) {
        Event event = new Event();
        event.setTitulo(req.titulo());
        event.setDescripcion(req.descripcion());
        event.setFechaInicio(req.fechaInicio());
        event.setFechaFin(req.fechaFin());
        event.setUbicacion(req.ubicacion());
        event.setAuthorEmail(authorEmail);
        return EventResponse.from(eventRepository.save(event));
    }

    public void delete(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
        eventRepository.deleteById(id);
    }
}
