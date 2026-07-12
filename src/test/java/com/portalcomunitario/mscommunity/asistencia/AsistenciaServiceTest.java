package com.portalcomunitario.mscommunity.asistencia;

import com.portalcomunitario.mscommunity.agrupacion.Agrupacion;
import com.portalcomunitario.mscommunity.agrupacion.AgrupacionRepository;
import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacion;
import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacionRepository;
import com.portalcomunitario.mscommunity.agrupacion.ReunionCancelada;
import com.portalcomunitario.mscommunity.agrupacion.ReunionCanceladaRepository;
import com.portalcomunitario.mscommunity.event.Event;
import com.portalcomunitario.mscommunity.event.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepo;
    @Mock private EventRepository eventRepo;
    @Mock private InscripcionAgrupacionRepository inscripcionRepo;
    @Mock private AgrupacionRepository agrupacionRepo;
    @Mock private ReunionCanceladaRepository reunionCanceladaRepo;

    private AsistenciaService service;

    @BeforeEach
    void setUp() {
        service = new AsistenciaService(asistenciaRepo, eventRepo, inscripcionRepo,
                agrupacionRepo, reunionCanceladaRepo);
    }

    private Event actividad(UUID eventoId, UUID agrupacionId) {
        Event e = new Event();
        e.setId(eventoId);
        e.setTitulo("Taller");
        e.setAgrupacionId(agrupacionId);
        return e;
    }

    private InscripcionAgrupacion socio(UUID agrupacionId, String email) {
        InscripcionAgrupacion i = new InscripcionAgrupacion();
        i.setAgrupacionId(agrupacionId);
        i.setVecinoEmail(email);
        return i;
    }

    @Test
    @DisplayName("deActividad: evento inexistente lanza 404")
    void deActividad_eventoNoExiste_404() {
        UUID eventoId = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deActividad(eventoId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("deActividad: evento sin agrupación lanza 400")
    void deActividad_sinAgrupacion_400() {
        UUID eventoId = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.of(actividad(eventoId, null)));

        assertThatThrownBy(() -> service.deActividad(eventoId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("agrupación");
    }

    @Test
    @DisplayName("deActividad: cada socio con su estado; ausente por defecto")
    void deActividad_estadoPorSocio() {
        UUID eventoId = UUID.randomUUID();
        UUID ag = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.of(actividad(eventoId, ag)));
        Asistencia presente = new Asistencia();
        presente.setVecinoEmail("a@x.com");
        presente.setPresente(true);
        when(asistenciaRepo.findByEventoId(eventoId)).thenReturn(List.of(presente));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(
                socio(ag, "a@x.com"), socio(ag, "b@x.com")));

        List<AsistenciaSocioDto> res = service.deActividad(eventoId);

        assertThat(res).hasSize(2);
        assertThat(res.get(0)).isEqualTo(new AsistenciaSocioDto("a@x.com", true));
        assertThat(res.get(1)).isEqualTo(new AsistenciaSocioDto("b@x.com", false));
    }

    @Test
    @DisplayName("marcar: los correos en 'presentes' quedan presente=true; el resto ausente")
    void marcar_guardaTodos() {
        UUID eventoId = UUID.randomUUID();
        UUID ag = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.of(actividad(eventoId, ag)));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(
                socio(ag, "a@x.com"), socio(ag, "b@x.com")));
        when(asistenciaRepo.findByEventoIdAndVecinoEmail(any(), any())).thenReturn(Optional.empty());

        service.marcar(eventoId, List.of("a@x.com"));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).isPresente()).isTrue();   // a@x.com
        assertThat(captor.getAllValues().get(0).getVecinoEmail()).isEqualTo("a@x.com");
        assertThat(captor.getAllValues().get(1).isPresente()).isFalse();  // b@x.com
    }

    @Test
    @DisplayName("marcar: lista nula se trata como vacía (todos ausentes)")
    void marcar_listaNula_todosAusentes() {
        UUID eventoId = UUID.randomUUID();
        UUID ag = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.of(actividad(eventoId, ag)));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(socio(ag, "a@x.com")));
        when(asistenciaRepo.findByEventoIdAndVecinoEmail(any(), any())).thenReturn(Optional.empty());

        service.marcar(eventoId, null);

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepo).save(captor.capture());
        assertThat(captor.getValue().isPresente()).isFalse();
    }

    @Test
    @DisplayName("marcar: reutiliza el registro de asistencia existente en vez de crear otro")
    void marcar_reutilizaRegistroExistente() {
        UUID eventoId = UUID.randomUUID();
        UUID ag = UUID.randomUUID();
        when(eventRepo.findById(eventoId)).thenReturn(Optional.of(actividad(eventoId, ag)));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(socio(ag, "a@x.com")));
        Asistencia existente = new Asistencia();
        existente.setEventoId(eventoId);
        existente.setVecinoEmail("a@x.com");
        existente.setPresente(false);
        when(asistenciaRepo.findByEventoIdAndVecinoEmail(eventoId, "a@x.com"))
                .thenReturn(Optional.of(existente));

        service.marcar(eventoId, List.of("a@x.com"));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepo).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(existente.isPresente()).isTrue();
    }

    @Test
    @DisplayName("miAsistencia: devuelve la asistencia propia del vecino en la agrupación")
    void miAsistencia_devuelvePropia() {
        UUID ag = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Asistencia a = new Asistencia();
        a.setEventoId(eventoId);
        a.setPresente(true);
        when(asistenciaRepo.findByAgrupacionIdAndVecinoEmail(ag, "a@x.com")).thenReturn(List.of(a));

        List<MiAsistenciaDto> res = service.miAsistencia(ag, "a@x.com");

        assertThat(res).containsExactly(new MiAsistenciaDto(eventoId, true));
    }

    // ---- Sesiones derivadas de la reunión periódica ----

    @Test
    @DisplayName("sesiones: agrupación sin día de reunión no tiene sesiones")
    void sesiones_sinReunion_vacio() {
        UUID ag = UUID.randomUUID();
        when(agrupacionRepo.findById(ag)).thenReturn(Optional.of(new Agrupacion()));

        assertThat(service.sesiones(ag)).isEmpty();
    }

    @Test
    @DisplayName("sesiones: genera ocurrencias en el día correcto, con hora, y excluye canceladas")
    void sesiones_generaOcurrencias_excluyeCanceladas() {
        UUID ag = UUID.randomUUID();
        Agrupacion a = new Agrupacion();
        a.setReunionDiaSemana(5); // viernes (ISO)
        a.setReunionHora(LocalTime.of(17, 0));
        when(agrupacionRepo.findById(ag)).thenReturn(Optional.of(a));
        LocalDate viernesReciente = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        ReunionCancelada c = new ReunionCancelada();
        c.setAgrupacionId(ag);
        c.setFecha(viernesReciente);
        when(reunionCanceladaRepo.findByAgrupacionId(ag)).thenReturn(List.of(c));

        List<SesionAsistenciaDto> res = service.sesiones(ag);

        assertThat(res).isNotEmpty();
        assertThat(res).allSatisfy(s -> {
            assertThat(s.fecha().getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
            assertThat(s.fecha()).isBeforeOrEqualTo(LocalDate.now());
            assertThat(s.hora()).isEqualTo("17:00");
        });
        assertThat(res).noneMatch(s -> s.fecha().equals(viernesReciente)); // cancelada excluida
    }

    @Test
    @DisplayName("sesionId: es determinístico por (agrupación, fecha) y distinto por fecha")
    void sesionId_deterministico() {
        UUID ag = UUID.randomUUID();
        LocalDate f = LocalDate.of(2026, 7, 10);
        assertThat(AsistenciaService.sesionId(ag, f)).isEqualTo(AsistenciaService.sesionId(ag, f));
        assertThat(AsistenciaService.sesionId(ag, f)).isNotEqualTo(AsistenciaService.sesionId(ag, f.plusWeeks(1)));
    }

    @Test
    @DisplayName("marcarSesion: guarda cada socio con eventoId=sesión y agrupación, sin buscar un evento")
    void marcarSesion_guardaPorSocio() {
        UUID ag = UUID.randomUUID();
        UUID ses = UUID.randomUUID();
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(
                socio(ag, "a@x.com"), socio(ag, "b@x.com")));
        when(asistenciaRepo.findByEventoIdAndVecinoEmail(any(), any())).thenReturn(Optional.empty());

        service.marcarSesion(ag, ses, List.of("a@x.com"));

        ArgumentCaptor<Asistencia> captor = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getEventoId()).isEqualTo(ses);
        assertThat(captor.getAllValues().get(0).getAgrupacionId()).isEqualTo(ag);
        assertThat(captor.getAllValues().get(0).isPresente()).isTrue();   // a@x.com
        assertThat(captor.getAllValues().get(1).isPresente()).isFalse();  // b@x.com
    }
}
