package com.portalcomunitario.mscommunity.event;

import com.portalcomunitario.mscommunity.contacto.ContactoClient;
import com.portalcomunitario.mscommunity.messaging.NotificacionEvento;
import com.portalcomunitario.mscommunity.messaging.NotificacionPublisher;
import com.portalcomunitario.mscommunity.messaging.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ContactoClient contactoClient;
    @Mock private NotificacionPublisher publisher;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, contactoClient, publisher);
    }

    private Event evento(UUID id, UUID agrupacionId) {
        Event e = new Event();
        e.setId(id);
        e.setTitulo("Feria de las pulgas");
        e.setDescripcion("En la plaza");
        e.setFechaInicio(LocalDateTime.of(2026, 8, 1, 10, 0));
        e.setUbicacion("Plaza central");
        e.setAgrupacionId(agrupacionId);
        return e;
    }

    private EventRequest req(boolean recurrente, String categoria) {
        return new EventRequest("Título", "Desc",
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 12, 0),
                "Plaza", categoria, "sub", "#abc", null, -33.4, -70.6,
                recurrente, "semanal", 2, LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("findAll: mapea todos los eventos ordenados por fecha")
    void findAll_mapea() {
        when(eventRepository.findAllByOrderByFechaInicioAsc())
                .thenReturn(List.of(evento(UUID.randomUUID(), null)));

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("findById: id inexistente lanza 404")
    void findById_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("create: aplica defaults (categoría GENERAL en inválida) y guarda con autor")
    void create_categoriaInvalida_usaGeneral() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse res = service.create(req(false, "no-existe"), "autor@x.com", "Autor");

        assertThat(res.categoria()).isEqualTo("GENERAL");
        assertThat(res.authorEmail()).isEqualTo("autor@x.com");
        assertThat(res.recurrente()).isFalse();
    }

    @Test
    @DisplayName("create (recurrente): normaliza frecuencia e intervalo")
    void create_recurrente_normaliza() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse res = service.create(req(true, "TALLER"), "autor@x.com", "Autor");

        assertThat(res.categoria()).isEqualTo("TALLER");
        assertThat(res.recurrente()).isTrue();
        assertThat(res.frecuencia()).isEqualTo("SEMANAL");
        assertThat(res.intervalo()).isEqualTo(2);
        assertThat(res.color()).isEqualTo("#abc");
    }

    @Test
    @DisplayName("update (no recurrente): limpia frecuencia/intervalo/recurrenciaFin")
    void update_noRecurrente_limpiaCampos() {
        UUID id = UUID.randomUUID();
        Event e = evento(id, null);
        e.setRecurrente(true);
        e.setFrecuencia("SEMANAL");
        e.setIntervalo(3);
        e.setRecurrenciaFin(LocalDate.of(2026, 12, 31));
        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse res = service.update(id, req(false, "GENERAL"));

        assertThat(res.recurrente()).isFalse();
        assertThat(res.frecuencia()).isNull();
        assertThat(res.intervalo()).isNull();
        assertThat(res.recurrenciaFin()).isNull();
    }

    @Test
    @DisplayName("update: id inexistente lanza 404")
    void update_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, req(false, "GENERAL")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("notificarComunidad: publica broadcast y marca la fecha de notificación")
    void notificarComunidad_ok() {
        UUID id = UUID.randomUUID();
        Event e = evento(id, null); // sin agrupación => evento de comunidad
        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contactoClient.todos()).thenReturn(List.of(
                new ContactoClient.Contacto("v@x.com", "Vecino", "+56 9", true)));

        EventResponse res = service.notificarComunidad(id);

        assertThat(res.notificadoComunidad()).isNotNull();
        ArgumentCaptor<NotificacionEvento> captor = ArgumentCaptor.forClass(NotificacionEvento.class);
        verify(publisher).publicar(eq(RabbitConfig.RK_EVENTO_COMUNIDAD), captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo("EVENTO_COMUNIDAD");
        assertThat(captor.getValue().destinatarios()).hasSize(1);
    }

    @Test
    @DisplayName("notificarComunidad: evento de agrupación no se difunde (400)")
    void notificarComunidad_eventoAgrupacion_400() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(evento(id, UUID.randomUUID())));

        assertThatThrownBy(() -> service.notificarComunidad(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("comunidad");
        verify(publisher, never()).publicar(any(), any());
    }

    @Test
    @DisplayName("notificarComunidad: id inexistente lanza 404")
    void notificarComunidad_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notificarComunidad(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("publicarRecordatorio: publica con la routing key de recordatorio")
    void publicarRecordatorio_ok() {
        Event e = evento(UUID.randomUUID(), null);
        when(contactoClient.todos()).thenReturn(List.of(
                new ContactoClient.Contacto("v@x.com", "Vecino", "+56 9", true)));

        service.publicarRecordatorio(e);

        ArgumentCaptor<NotificacionEvento> captor = ArgumentCaptor.forClass(NotificacionEvento.class);
        verify(publisher).publicar(eq(RabbitConfig.RK_EVENTO_RECORDATORIO), captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo("EVENTO_RECORDATORIO");
        assertThat(captor.getValue().titulo()).startsWith("Recordatorio: ");
    }

    @Test
    @DisplayName("delete: id inexistente lanza 404 y no borra")
    void delete_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(eventRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class);
        verify(eventRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete: id existente borra")
    void delete_ok() {
        UUID id = UUID.randomUUID();
        when(eventRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(eventRepository).deleteById(id);
    }

    @Test
    @DisplayName("create: color inválido (no #hex) se normaliza a null")
    void create_colorInvalido_null() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "rojo",
                null, null, null, false, null, null, null);
        EventResponse res = service.create(r, "autor@x.com", "Autor");

        assertThat(res.color()).isNull();
    }

    @Test
    @DisplayName("create (recurrente): frecuencia inválida cae a SEMANAL e intervalo <=0 se corrige a 1")
    void create_recurrente_frecuenciaInvalida_intervaloCero() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "#abc",
                null, null, null, true, "quincenal", 0, LocalDate.now().plusMonths(1));
        EventResponse res = service.create(r, "autor@x.com", "Autor");

        assertThat(res.frecuencia()).isEqualTo("SEMANAL");
        assertThat(res.intervalo()).isEqualTo(1);
    }

    @Test
    @DisplayName("create: subcategoría en blanco se guarda como null")
    void create_subcategoriaBlank_null() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "   ", "#abc",
                null, null, null, false, null, null, null);
        EventResponse res = service.create(r, "autor@x.com", "Autor");

        assertThat(res.subcategoria()).isNull();
    }

    @Test
    @DisplayName("update (recurrente): normaliza frecuencia e intervalo del período")
    void update_recurrente_normaliza() {
        UUID id = UUID.randomUUID();
        Event e = evento(id, null); // parte no recurrente
        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse res = service.update(id, req(true, "TALLER"));

        assertThat(res.recurrente()).isTrue();
        assertThat(res.frecuencia()).isEqualTo("SEMANAL");
        assertThat(res.intervalo()).isEqualTo(2);
    }

    @Test
    @DisplayName("create (recurrente): frecuencia DIARIA se conserva")
    void create_recurrente_frecuenciaDiaria() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "#abc",
                null, null, null, true, "diaria", 1, LocalDate.now().plusMonths(1));

        assertThat(service.create(r, "a@x.com", "A").frecuencia()).isEqualTo("DIARIA");
    }

    @Test
    @DisplayName("create (recurrente): frecuencia MENSUAL se conserva")
    void create_recurrente_frecuenciaMensual() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "#abc",
                null, null, null, true, "MENSUAL", 1, LocalDate.now().plusMonths(1));

        assertThat(service.create(r, "a@x.com", "A").frecuencia()).isEqualTo("MENSUAL");
    }

    @Test
    @DisplayName("create (recurrente): frecuencia ANUAL se conserva")
    void create_recurrente_frecuenciaAnual() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "#abc",
                null, null, null, true, "anual", 1, LocalDate.now().plusMonths(1));

        assertThat(service.create(r, "a@x.com", "A").frecuencia()).isEqualTo("ANUAL");
    }

    @Test
    @DisplayName("create (recurrente): frecuencia nula cae a SEMANAL")
    void create_recurrente_frecuenciaNull_semanal() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", "GENERAL", "sub", "#abc",
                null, null, null, true, null, 1, LocalDate.now().plusMonths(1));

        assertThat(service.create(r, "a@x.com", "A").frecuencia()).isEqualTo("SEMANAL");
    }

    @Test
    @DisplayName("create: categoría nula usa GENERAL y color nulo queda en null")
    void create_categoriaNull_colorNull() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRequest r = new EventRequest("t", "d",
                LocalDateTime.now().plusDays(1), null, "Plaza", null, "sub", null,
                null, null, null, false, null, null, null);
        EventResponse res = service.create(r, "a@x.com", "A");

        assertThat(res.categoria()).isEqualTo("GENERAL");
        assertThat(res.color()).isNull();
    }

    @Test
    @DisplayName("publicarRecordatorio: evento sin fecha/ubicación/descripción publica igual (ramas vacías)")
    void publicarRecordatorio_camposOpcionalesVacios() {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setTitulo("Asamblea");
        // sin fechaInicio, sin ubicación, sin descripción
        when(contactoClient.todos()).thenReturn(List.of(
                new ContactoClient.Contacto("v@x.com", "Vecino", "+56 9", true)));

        service.publicarRecordatorio(e);

        ArgumentCaptor<NotificacionEvento> captor = ArgumentCaptor.forClass(NotificacionEvento.class);
        verify(publisher).publicar(eq(RabbitConfig.RK_EVENTO_RECORDATORIO), captor.capture());
        assertThat(captor.getValue().titulo()).startsWith("Recordatorio: ");
        assertThat(captor.getValue().mensaje()).contains("Asamblea");
    }

}
