package com.portalcomunitario.mscommunity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoRecordatorioSchedulerTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventService eventService;

    private EventoRecordatorioScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EventoRecordatorioScheduler(eventRepository, eventService);
    }

    private Event evento(LocalDateTime fechaInicio) {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setTitulo("Evento");
        e.setFechaInicio(fechaInicio);
        e.setNotificadoComunidad(LocalDateTime.now());
        e.setRecordatorioEnviado(false);
        return e;
    }

    @Test
    @DisplayName("revisar: evento de mañana publica recordatorio y queda marcado como enviado")
    void revisar_eventoManiana_publica() {
        Event maniana = evento(LocalDate.now().plusDays(1).atTime(LocalTime.of(18, 0)));
        when(eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse())
                .thenReturn(List.of(maniana));

        scheduler.revisar();

        verify(eventService).publicarRecordatorio(maniana);
        verify(eventRepository).save(maniana);
        assertThat(maniana.isRecordatorioEnviado()).isTrue();
    }

    @Test
    @DisplayName("revisar: evento sin fecha se marca enviado sin publicar")
    void revisar_sinFecha_marcaSinPublicar() {
        Event sinFecha = evento(null);
        when(eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse())
                .thenReturn(List.of(sinFecha));

        scheduler.revisar();

        verify(eventService, never()).publicarRecordatorio(sinFecha);
        verify(eventRepository).save(sinFecha);
        assertThat(sinFecha.isRecordatorioEnviado()).isTrue();
    }

    @Test
    @DisplayName("revisar: evento ya pasado (ventana vencida) se marca sin publicar")
    void revisar_eventoPasado_marcaSinPublicar() {
        Event pasado = evento(LocalDate.now().minusDays(2).atTime(LocalTime.of(10, 0)));
        when(eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse())
                .thenReturn(List.of(pasado));

        scheduler.revisar();

        verify(eventService, never()).publicarRecordatorio(pasado);
        verify(eventRepository).save(pasado);
        assertThat(pasado.isRecordatorioEnviado()).isTrue();
    }

    @Test
    @DisplayName("revisar: evento futuro (más allá de mañana) queda pendiente")
    void revisar_eventoFuturo_pendiente() {
        Event futuro = evento(LocalDate.now().plusDays(10).atTime(LocalTime.of(10, 0)));
        when(eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse())
                .thenReturn(List.of(futuro));

        scheduler.revisar();

        verify(eventService, never()).publicarRecordatorio(futuro);
        verify(eventRepository, never()).save(futuro);
        assertThat(futuro.isRecordatorioEnviado()).isFalse();
    }

    @Test
    @DisplayName("revisar: mezcla de eventos procesa cada uno según su fecha")
    void revisar_mezcla() {
        Event maniana = evento(LocalDate.now().plusDays(1).atTime(LocalTime.of(9, 0)));
        Event futuro = evento(LocalDate.now().plusDays(5).atTime(LocalTime.of(9, 0)));
        when(eventRepository.findByNotificadoComunidadIsNotNullAndRecordatorioEnviadoFalse())
                .thenReturn(List.of(maniana, futuro));

        scheduler.revisar();

        verify(eventService, times(1)).publicarRecordatorio(maniana);
        verify(eventService, never()).publicarRecordatorio(futuro);
    }
}
