package com.portalcomunitario.mscommunity.cuota;

import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacion;
import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaServiceTest {

    @Mock private CuotaPeriodoRepository periodoRepo;
    @Mock private CuotaRepository cuotaRepo;
    @Mock private InscripcionAgrupacionRepository inscripcionRepo;

    private CuotaService service;

    @BeforeEach
    void setUp() {
        service = new CuotaService(periodoRepo, cuotaRepo, inscripcionRepo);
    }

    private CuotaRequest req(Integer monto, String periodicidad, LocalDate ini, LocalDate fin) {
        return new CuotaRequest(monto, periodicidad, ini, fin);
    }

    private InscripcionAgrupacion inscripcion(UUID agrupacionId, String email) {
        InscripcionAgrupacion i = new InscripcionAgrupacion();
        i.setAgrupacionId(agrupacionId);
        i.setVecinoEmail(email);
        return i;
    }

    @Test
    @DisplayName("periodoActivo: devuelve el período ABIERTA o null")
    void periodoActivo_devuelve() {
        UUID ag = UUID.randomUUID();
        CuotaPeriodo p = new CuotaPeriodo();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));

        assertThat(service.periodoActivo(ag)).isSameAs(p);
    }

    @Test
    @DisplayName("activar: si ya hay período abierto lanza 409")
    void activar_yaAbierto_409() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(new CuotaPeriodo()));

        assertThatThrownBy(() -> service.activar(ag, req(1000, "MENSUAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("abierto");
        verify(periodoRepo, never()).save(any());
    }

    @Test
    @DisplayName("activar: monto <= 0 lanza 400")
    void activar_montoInvalido_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(0, "MENSUAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidos");
    }

    @Test
    @DisplayName("activar: fechaFin antes de fechaInicio lanza 400")
    void activar_fechasInvertidas_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(1000, "MENSUAL",
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 1, 1))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("activar (MENSUAL): crea una cuota por mes para cada socio")
    void activar_mensual_generaCuotas() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());
        when(periodoRepo.save(any(CuotaPeriodo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(inscripcion(ag, "a@x.com")));

        CuotaPeriodo p = service.activar(ag, req(1000, "mensual",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)));

        assertThat(p.getPeriodicidad()).isEqualTo(Periodicidad.MENSUAL);
        // Ene, Feb, Mar -> 3 cuotas para el único socio.
        verify(cuotaRepo, times(3)).save(any(Cuota.class));
    }

    @Test
    @DisplayName("activar (SEMANAL): crea una cuota por semana")
    void activar_semanal_generaCuotas() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());
        when(periodoRepo.save(any(CuotaPeriodo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(inscripcion(ag, "a@x.com")));

        service.activar(ag, req(500, "SEMANAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14)));

        // 01-01 y 08-01 -> 2 semanas.
        verify(cuotaRepo, times(2)).save(any(Cuota.class));
    }

    @Test
    @DisplayName("activar: periodicidad inválida lanza 400")
    void activar_periodicidadInvalida_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(500, "QUINCENAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Periodicidad");
    }

    @Test
    @DisplayName("cerrar: pasa el período a CERRADA")
    void cerrar_ok() {
        UUID ag = UUID.randomUUID();
        CuotaPeriodo p = new CuotaPeriodo();
        p.setEstado(EstadoPeriodo.ABIERTA);
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));
        when(periodoRepo.save(p)).thenReturn(p);

        CuotaPeriodo res = service.cerrar(ag);

        assertThat(res.getEstado()).isEqualTo(EstadoPeriodo.CERRADA);
    }

    @Test
    @DisplayName("cerrar: sin período abierto lanza 404")
    void cerrar_sinPeriodo_404() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cerrar(ag))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay período");
    }

    @Test
    @DisplayName("actualizarMonto: propaga el nuevo monto solo a las cuotas NO pagadas del período")
    void actualizarMonto_propagaANoPagadas() {
        UUID ag = UUID.randomUUID();
        UUID pid = UUID.randomUUID();
        CuotaPeriodo p = mock(CuotaPeriodo.class);
        when(p.getId()).thenReturn(pid);
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));

        Cuota impaga = new Cuota();
        impaga.setPeriodoId(pid);
        impaga.setPagada(false);
        impaga.setMonto(1000);
        Cuota pagada = new Cuota();
        pagada.setPeriodoId(pid);
        pagada.setPagada(true);
        pagada.setMonto(1000);
        when(cuotaRepo.findByAgrupacionIdOrderByVencimientoAsc(ag)).thenReturn(List.of(impaga, pagada));

        service.actualizarMonto(ag, 2000);

        assertThat(impaga.getMonto()).isEqualTo(2000);
        assertThat(pagada.getMonto()).isEqualTo(1000);
        verify(cuotaRepo, times(1)).save(any(Cuota.class));
    }

    @Test
    @DisplayName("actualizarMonto: sin período abierto lanza 404")
    void actualizarMonto_sinPeriodo_404() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarMonto(ag, 2000))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("actualizarMonto: monto <= 0 lanza 400")
    void actualizarMonto_montoInvalido_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(new CuotaPeriodo()));

        assertThatThrownBy(() -> service.actualizarMonto(ag, 0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    @DisplayName("generarParaNuevoSocio: sin período abierto no genera nada")
    void generarParaNuevoSocio_sinPeriodo_noHaceNada() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        service.generarParaNuevoSocio(ag, "a@x.com");

        verify(cuotaRepo, never()).save(any());
    }

    @Test
    @DisplayName("generarParaNuevoSocio: si el socio ya tiene cuotas no duplica")
    void generarParaNuevoSocio_yaTieneCuotas_noDuplica() {
        UUID ag = UUID.randomUUID();
        CuotaPeriodo p = new CuotaPeriodo();
        p.setPeriodicidad(Periodicidad.MENSUAL);
        p.setMonto(1000);
        p.setFechaInicio(LocalDate.of(2026, 1, 1));
        p.setFechaFin(LocalDate.of(2026, 12, 31));
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));
        when(cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(ag, "a@x.com"))
                .thenReturn(List.of(new Cuota()));

        service.generarParaNuevoSocio(ag, "a@x.com");

        verify(cuotaRepo, never()).save(any());
    }

    @Test
    @DisplayName("generarParaNuevoSocio: socio nuevo recibe sus cuotas")
    void generarParaNuevoSocio_nuevo_genera() {
        UUID ag = UUID.randomUUID();
        CuotaPeriodo p = new CuotaPeriodo();
        p.setPeriodicidad(Periodicidad.MENSUAL);
        p.setMonto(1000);
        p.setFechaInicio(LocalDate.now().minusMonths(1));
        p.setFechaFin(LocalDate.now().plusMonths(2));
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));
        when(cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(ag, "a@x.com"))
                .thenReturn(List.of());

        service.generarParaNuevoSocio(ag, "a@x.com");

        verify(cuotaRepo, org.mockito.Mockito.atLeastOnce()).save(any(Cuota.class));
    }

    @Test
    @DisplayName("marcarPago (pagada=true): marca la cuota y fija la fecha de pago")
    void marcarPago_pagada() {
        UUID cid = UUID.randomUUID();
        Cuota c = new Cuota();
        c.setPagada(false);
        when(cuotaRepo.findById(cid)).thenReturn(Optional.of(c));
        when(cuotaRepo.save(c)).thenReturn(c);

        Cuota res = service.marcarPago(cid, true);

        assertThat(res.isPagada()).isTrue();
        assertThat(res.getFechaPago()).isNotNull();
    }

    @Test
    @DisplayName("marcarPago (pagada=false): revierte y borra la fecha de pago")
    void marcarPago_pendiente() {
        UUID cid = UUID.randomUUID();
        Cuota c = new Cuota();
        c.setPagada(true);
        c.setFechaPago(java.time.LocalDateTime.now());
        when(cuotaRepo.findById(cid)).thenReturn(Optional.of(c));
        when(cuotaRepo.save(c)).thenReturn(c);

        Cuota res = service.marcarPago(cid, false);

        assertThat(res.isPagada()).isFalse();
        assertThat(res.getFechaPago()).isNull();
    }

    @Test
    @DisplayName("marcarPago: cuota inexistente lanza 404")
    void marcarPago_noExiste_404() {
        UUID cid = UUID.randomUUID();
        when(cuotaRepo.findById(cid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarPago(cid, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("deAgrupacion / misCuotas: delegan en el repositorio")
    void consultas_delegan() {
        UUID ag = UUID.randomUUID();
        when(cuotaRepo.findByAgrupacionIdOrderByVencimientoAsc(ag)).thenReturn(List.of(new Cuota()));
        when(cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(ag, "a@x.com"))
                .thenReturn(List.of(new Cuota(), new Cuota()));

        assertThat(service.deAgrupacion(ag)).hasSize(1);
        assertThat(service.misCuotas(ag, "a@x.com")).hasSize(2);
    }

    @Test
    @DisplayName("activar: monto null lanza 400")
    void activar_montoNull_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(null, "MENSUAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidos");
        verify(periodoRepo, never()).save(any());
    }

    @Test
    @DisplayName("activar: periodicidad null lanza 400")
    void activar_periodicidadNull_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(1000, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidos");
        verify(periodoRepo, never()).save(any());
    }

    @Test
    @DisplayName("activar: fechaInicio null lanza 400")
    void activar_fechaInicioNull_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(1000, "MENSUAL",
                null, LocalDate.of(2026, 3, 31))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidos");
        verify(periodoRepo, never()).save(any());
    }

    @Test
    @DisplayName("activar: fechaFin null lanza 400")
    void activar_fechaFinNull_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(ag, req(1000, "MENSUAL",
                LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválidos");
        verify(periodoRepo, never()).save(any());
    }

    @Test
    @DisplayName("activar (SEMANAL): la última semana recorta el vencimiento al fin del período")
    void activar_semanal_recortaUltimaSemana() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());
        when(periodoRepo.save(any(CuotaPeriodo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(inscripcion(ag, "a@x.com")));

        // 01-01 (venc 01-07) y 08-01 (venc 01-14 recortado a 01-10) -> 2 cuotas
        service.activar(ag, req(500, "SEMANAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)));

        ArgumentCaptor<Cuota> captor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepo, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getVencimiento()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    @DisplayName("activar (MENSUAL): el mes recorta el vencimiento al fin del período")
    void activar_mensual_recortaAlFin() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.empty());
        when(periodoRepo.save(any(CuotaPeriodo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionRepo.findByAgrupacionId(ag)).thenReturn(List.of(inscripcion(ag, "a@x.com")));

        // Único mes con fin 01-15 (antes de fin de mes) -> venc recortado a 01-15
        service.activar(ag, req(1000, "MENSUAL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)));

        ArgumentCaptor<Cuota> captor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getVencimiento()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("actualizarMonto: monto null lanza 400")
    void actualizarMonto_montoNull_400() {
        UUID ag = UUID.randomUUID();
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(new CuotaPeriodo()));

        assertThatThrownBy(() -> service.actualizarMonto(ag, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mayor a 0");
    }

    @Test
    @DisplayName("actualizarMonto: una cuota de OTRO período no se modifica")
    void actualizarMonto_cuotaDeOtroPeriodo_noSeModifica() {
        UUID ag = UUID.randomUUID();
        UUID pid = UUID.randomUUID();
        CuotaPeriodo p = mock(CuotaPeriodo.class);
        when(p.getId()).thenReturn(pid);
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));

        Cuota otroPeriodo = new Cuota();
        otroPeriodo.setPeriodoId(UUID.randomUUID()); // distinto de pid
        otroPeriodo.setPagada(false);
        otroPeriodo.setMonto(1000);
        when(cuotaRepo.findByAgrupacionIdOrderByVencimientoAsc(ag)).thenReturn(List.of(otroPeriodo));

        service.actualizarMonto(ag, 2000);

        assertThat(otroPeriodo.getMonto()).isEqualTo(1000);
        verify(cuotaRepo, never()).save(any());
    }

    @Test
    @DisplayName("generarParaNuevoSocio: período que inicia a futuro usa la fechaInicio (no hoy)")
    void generarParaNuevoSocio_fechaInicioFutura_usaFechaInicio() {
        UUID ag = UUID.randomUUID();
        CuotaPeriodo p = new CuotaPeriodo();
        p.setPeriodicidad(Periodicidad.MENSUAL);
        p.setMonto(1000);
        p.setFechaInicio(LocalDate.now().plusMonths(1));
        p.setFechaFin(LocalDate.now().plusMonths(3));
        when(periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(ag, EstadoPeriodo.ABIERTA))
                .thenReturn(Optional.of(p));
        when(cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(ag, "a@x.com"))
                .thenReturn(List.of());

        service.generarParaNuevoSocio(ag, "a@x.com");

        verify(cuotaRepo, org.mockito.Mockito.atLeastOnce()).save(any(Cuota.class));
    }
}
