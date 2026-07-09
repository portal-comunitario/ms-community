package com.portalcomunitario.mscommunity.agrupacion;

import com.portalcomunitario.mscommunity.cuota.CuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgrupacionServiceTest {

    @Mock private AgrupacionRepository repository;
    @Mock private InscripcionAgrupacionRepository inscripcionRepository;
    @Mock private ReunionCanceladaRepository reunionCanceladaRepository;
    @Mock private CuotaService cuotaService;

    private AgrupacionService service;

    @BeforeEach
    void setUp() {
        service = new AgrupacionService(repository, inscripcionRepository,
                reunionCanceladaRepository, cuotaService);
    }

    private Agrupacion agrupacion(UUID id, String nombre) {
        Agrupacion a = new Agrupacion();
        a.setNombre(nombre);
        a.setDescripcion("desc");
        a.setResponsable("Ana");
        if (id != null) {
            try {
                var f = Agrupacion.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(a, id);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return a;
    }

    private AgrupacionRequest req() {
        return new AgrupacionRequest("Taller de pintura", "Óleo y acuarela", "Ana",
                3, "18:30", null, null);
    }

    @Test
    @DisplayName("findAll: marca inscrito=true en las agrupaciones donde el vecino está inscrito")
    void findAll_marcaInscrito() {
        UUID inscritaId = UUID.randomUUID();
        UUID otraId = UUID.randomUUID();
        InscripcionAgrupacion insc = new InscripcionAgrupacion();
        insc.setAgrupacionId(inscritaId);
        insc.setVecinoEmail("vecino@x.com");
        when(inscripcionRepository.findByVecinoEmail("vecino@x.com")).thenReturn(List.of(insc));
        when(repository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                agrupacion(inscritaId, "A"), agrupacion(otraId, "B")));

        List<AgrupacionResponse> res = service.findAll("vecino@x.com");

        assertThat(res).hasSize(2);
        assertThat(res.get(0).inscrito()).isTrue();
        assertThat(res.get(1).inscrito()).isFalse();
    }

    @Test
    @DisplayName("create: guarda la agrupación con día normalizado y hora parseada")
    void create_ok() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(req());

        ArgumentCaptor<Agrupacion> captor = ArgumentCaptor.forClass(Agrupacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Taller de pintura");
        assertThat(captor.getValue().getReunionDiaSemana()).isEqualTo(3);
        assertThat(captor.getValue().getReunionHora()).isEqualTo(LocalTime.of(18, 30));
    }

    @Test
    @DisplayName("create: día fuera de rango (1..7) y hora inválida quedan en null")
    void create_diaYHoraInvalidos_quedanNull() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new AgrupacionRequest("X", "d", "r", 9, "no-es-hora", null, null));

        ArgumentCaptor<Agrupacion> captor = ArgumentCaptor.forClass(Agrupacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReunionDiaSemana()).isNull();
        assertThat(captor.getValue().getReunionHora()).isNull();
    }

    @Test
    @DisplayName("update: aplica cambios y refleja si el solicitante está inscrito")
    void update_ok() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "Vieja")));
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepository.existsByAgrupacionIdAndVecinoEmail(id, "vecino@x.com")).thenReturn(true);

        AgrupacionResponse res = service.update(id, req(), "vecino@x.com");

        assertThat(res.inscrito()).isTrue();
        assertThat(res.nombre()).isEqualTo("Taller de pintura");
    }

    @Test
    @DisplayName("update: id inexistente lanza 404")
    void update_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.update(id, req(), "vecino@x.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("delete: si no existe lanza 404 y no borra")
    void delete_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("inscribirse: socio nuevo se guarda y se le generan cuotas del período abierto")
    void inscribirse_nuevo_generaCuotas() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "A")));
        when(inscripcionRepository.existsByAgrupacionIdAndVecinoEmail(id, "vecino@x.com")).thenReturn(false);

        service.inscribirse(id, "vecino@x.com");

        verify(inscripcionRepository).save(any(InscripcionAgrupacion.class));
        verify(cuotaService).generarParaNuevoSocio(id, "vecino@x.com");
    }

    @Test
    @DisplayName("inscribirse: si ya está inscrito no guarda ni genera cuotas")
    void inscribirse_yaInscrito_noHaceNada() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "A")));
        when(inscripcionRepository.existsByAgrupacionIdAndVecinoEmail(id, "vecino@x.com")).thenReturn(true);

        service.inscribirse(id, "vecino@x.com");

        verify(inscripcionRepository, never()).save(any());
        verify(cuotaService, never()).generarParaNuevoSocio(any(), any());
    }

    @Test
    @DisplayName("inscribirse: agrupación inexistente lanza 404")
    void inscribirse_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.inscribirse(id, "vecino@x.com"))
                .isInstanceOf(ResponseStatusException.class);
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("salir: elimina la inscripción del vecino")
    void salir_eliminaInscripcion() {
        UUID id = UUID.randomUUID();

        service.salir(id, "vecino@x.com");

        verify(inscripcionRepository).deleteByAgrupacionIdAndVecinoEmail(id, "vecino@x.com");
    }

    @Test
    @DisplayName("misInscripciones: devuelve los ids de agrupación del vecino")
    void misInscripciones_devuelveIds() {
        UUID a1 = UUID.randomUUID();
        InscripcionAgrupacion i = new InscripcionAgrupacion();
        i.setAgrupacionId(a1);
        i.setVecinoEmail("vecino@x.com");
        when(inscripcionRepository.findByVecinoEmail("vecino@x.com")).thenReturn(List.of(i));

        assertThat(service.misInscripciones("vecino@x.com")).containsExactly(a1);
    }

    @Test
    @DisplayName("socios: devuelve los correos inscritos en la agrupación")
    void socios_devuelveEmails() {
        UUID id = UUID.randomUUID();
        InscripcionAgrupacion i = new InscripcionAgrupacion();
        i.setAgrupacionId(id);
        i.setVecinoEmail("a@x.com");
        when(inscripcionRepository.findByAgrupacionId(id)).thenReturn(List.of(i));

        assertThat(service.socios(id)).containsExactly("a@x.com");
    }

    @Test
    @DisplayName("cancelarReunion: fecha nula lanza 400")
    void cancelarReunion_fechaNula_400() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "A")));

        assertThatThrownBy(() -> service.cancelarReunion(id, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fecha");
        verify(reunionCanceladaRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarReunion: fecha nueva guarda la cancelación")
    void cancelarReunion_nueva_guarda() {
        UUID id = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 7, 15);
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "A")));
        when(reunionCanceladaRepository.existsByAgrupacionIdAndFecha(id, fecha)).thenReturn(false);

        service.cancelarReunion(id, fecha);

        verify(reunionCanceladaRepository).save(any(ReunionCancelada.class));
    }

    @Test
    @DisplayName("cancelarReunion: fecha ya cancelada no guarda de nuevo (idempotente)")
    void cancelarReunion_yaCancelada_noGuarda() {
        UUID id = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 7, 15);
        when(repository.findById(id)).thenReturn(java.util.Optional.of(agrupacion(id, "A")));
        when(reunionCanceladaRepository.existsByAgrupacionIdAndFecha(id, fecha)).thenReturn(true);

        service.cancelarReunion(id, fecha);

        verify(reunionCanceladaRepository, never()).save(any());
    }

    @Test
    @DisplayName("reactivarReunion: elimina la cancelación de esa fecha")
    void reactivarReunion_elimina() {
        UUID id = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 7, 15);

        service.reactivarReunion(id, fecha);

        verify(reunionCanceladaRepository).deleteByAgrupacionIdAndFecha(eq(id), eq(fecha));
    }

    @Test
    @DisplayName("create: día nulo y hora nula quedan en null (normalización)")
    void create_diaNuloHoraNula_quedanNull() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new AgrupacionRequest("X", "d", "r", null, null, null, null));

        ArgumentCaptor<Agrupacion> captor = ArgumentCaptor.forClass(Agrupacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReunionDiaSemana()).isNull();
        assertThat(captor.getValue().getReunionHora()).isNull();
    }

    @Test
    @DisplayName("create: día menor a 1 se normaliza a null")
    void create_diaMenorA1_null() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new AgrupacionRequest("X", "d", "r", 0, "18:00", null, null));

        ArgumentCaptor<Agrupacion> captor = ArgumentCaptor.forClass(Agrupacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReunionDiaSemana()).isNull();
    }

    @Test
    @DisplayName("create: hora en blanco se normaliza a null")
    void create_horaBlanco_null() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new AgrupacionRequest("X", "d", "r", 3, "   ", null, null));

        ArgumentCaptor<Agrupacion> captor = ArgumentCaptor.forClass(Agrupacion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReunionHora()).isNull();
    }

    @Test
    @DisplayName("create: con pausa la respuesta expone pausaInicio/pausaFin como texto")
    void create_conPausa_exponeFechas() {
        when(repository.save(any(Agrupacion.class))).thenAnswer(inv -> inv.getArgument(0));

        AgrupacionResponse res = service.create(new AgrupacionRequest(
                "X", "d", "r", 3, "18:00",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15)));

        assertThat(res.pausaInicio()).isEqualTo("2026-07-01");
        assertThat(res.pausaFin()).isEqualTo("2026-07-15");
    }
}
