package com.portalcomunitario.mscommunity.event;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoints de prueba (dev) para difusión/recordatorio de eventos, sin JWT. */
@RestController
@RequestMapping("/test/eventos")
public class TestEventoController {

    private final EventService eventService;
    private final EventoRecordatorioScheduler recordatorioScheduler;

    public TestEventoController(EventService eventService, EventoRecordatorioScheduler recordatorioScheduler) {
        this.eventService = eventService;
        this.recordatorioScheduler = recordatorioScheduler;
    }

    @PostMapping("/{id}/notificar")
    public EventResponse notificar(@PathVariable UUID id) {
        return eventService.notificarComunidad(id);
    }

    @PostMapping("/recordatorios")
    public String recordatorios() {
        recordatorioScheduler.revisar();
        return "Revisión de recordatorios de evento ejecutada (mira los logs)";
    }
}
