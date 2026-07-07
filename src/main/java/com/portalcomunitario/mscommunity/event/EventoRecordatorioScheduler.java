package com.portalcomunitario.mscommunity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Recordatorio del día antes para eventos de comunidad ya difundidos.
 * Diario: si un evento notificado ocurre mañana y aún no tiene recordatorio, lo publica.
 * Si la ventana ya pasó (evento hoy o antes), lo marca para no reintentar.
 */
@Component
public class EventoRecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventoRecordatorioScheduler.class);

    private final EventRepository eventRepository;
    private final EventService eventService;

    public EventoRecordatorioScheduler(EventRepository eventRepository, EventService eventService) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 15 9 * * *")
    @Transactional
    public void revisar() {
        LocalDate maniana = LocalDate.now().plusDays(1);
        int enviados = 0;
        for (Event ev : eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse()) {
            if (ev.getFechaInicio() == null) {
                ev.setRecordatorioEnviado(true);
                eventRepository.save(ev);
                continue;
            }
            LocalDate fecha = ev.getFechaInicio().toLocalDate();
            if (fecha.isEqual(maniana)) {
                eventService.publicarRecordatorio(ev);
                ev.setRecordatorioEnviado(true);
                eventRepository.save(ev);
                enviados++;
            } else if (fecha.isBefore(maniana)) {
                // Ventana pasada (evento hoy o antes): marcar sin enviar.
                ev.setRecordatorioEnviado(true);
                eventRepository.save(ev);
            }
            // fecha > mañana: aún no corresponde, queda pendiente.
        }
        log.info("Recordatorios de evento: {} enviado(s)", enviados);
    }
}
