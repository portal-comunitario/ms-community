package com.portalcomunitario.mscommunity.cuota;

import com.portalcomunitario.mscommunity.agrupacion.Agrupacion;
import com.portalcomunitario.mscommunity.agrupacion.AgrupacionRepository;
import com.portalcomunitario.mscommunity.contacto.ContactoClient;
import com.portalcomunitario.mscommunity.messaging.NotificacionPublisher;
import com.portalcomunitario.mscommunity.messaging.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaRecordatorioSchedulerTest {

    @Mock private CuotaPeriodoRepository periodoRepo;
    @Mock private CuotaRepository cuotaRepo;
    @Mock private RecordatorioCuotaRepository recordatorioRepo;
    @Mock private AgrupacionRepository agrupacionRepo;
    @Mock private ContactoClient contactoClient;
    @Mock private NotificacionPublisher publisher;

    private CuotaRecordatorioScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CuotaRecordatorioScheduler(periodoRepo, cuotaRepo, recordatorioRepo,
                agrupacionRepo, contactoClient, publisher);
    }

    private Cuota cuotaImpaga(String email, LocalDate venc) {
        Cuota c = new Cuota();
        c.setVecinoEmail(email);
        c.setPagada(false);
        c.setVencimiento(venc);
        c.setMonto(1000);
        c.setEtiqueta("Enero 2026");
        return c;
    }

    private CuotaPeriodo periodoAbierto(UUID agrupacionId) {
        CuotaPeriodo p = new CuotaPeriodo();
        p.setAgrupacionId(agrupacionId);
        p.setEstado(EstadoPeriodo.ABIERTA);
        return p;
    }

    @Test
    @DisplayName("revisar: con 2+ cuotas pendientes publica el recordatorio y guarda el nivel")
    void revisar_umbralAlcanzado_publica() {
        UUID ag = UUID.randomUUID();
        LocalDate ayer = LocalDate.now().minusDays(1);
        when(periodoRepo.findByEstado(EstadoPeriodo.ABIERTA)).thenReturn(List.of(periodoAbierto(ag)));
        when(cuotaRepo.findByPeriodoId(any())).thenReturn(List.of(
                cuotaImpaga("a@x.com", ayer), cuotaImpaga("a@x.com", ayer)));
        when(recordatorioRepo.findByPeriodoIdAndVecinoEmail(any(), eq("a@x.com")))
                .thenReturn(Optional.empty());
        when(contactoClient.porEmail("a@x.com"))
                .thenReturn(new ContactoClient.Contacto("a@x.com", "Ana", "+56 9", true));
        Agrupacion agr = new Agrupacion();
        agr.setNombre("Taller");
        when(agrupacionRepo.findById(ag)).thenReturn(Optional.of(agr));

        scheduler.revisar();

        verify(publisher).publicar(eq(RabbitConfig.RK_CUOTA_PENDIENTE), any());
        verify(recordatorioRepo).save(any(RecordatorioCuota.class));
    }

    @Test
    @DisplayName("revisar: con una sola cuota pendiente (bajo umbral) no publica")
    void revisar_bajoUmbral_noPublica() {
        UUID ag = UUID.randomUUID();
        LocalDate ayer = LocalDate.now().minusDays(1);
        when(periodoRepo.findByEstado(EstadoPeriodo.ABIERTA)).thenReturn(List.of(periodoAbierto(ag)));
        when(cuotaRepo.findByPeriodoId(any())).thenReturn(List.of(cuotaImpaga("a@x.com", ayer)));
        when(recordatorioRepo.findByPeriodoIdAndVecinoEmail(any(), eq("a@x.com")))
                .thenReturn(Optional.empty());

        scheduler.revisar();

        verify(publisher, never()).publicar(any(), any());
        verify(recordatorioRepo, never()).save(any());
    }

    @Test
    @DisplayName("revisar: si el socio pagó y bajó el nivel, se re-arma sin publicar")
    void revisar_nivelBaja_reArma() {
        UUID ag = UUID.randomUUID();
        LocalDate ayer = LocalDate.now().minusDays(1);
        when(periodoRepo.findByEstado(EstadoPeriodo.ABIERTA)).thenReturn(List.of(periodoAbierto(ag)));
        when(cuotaRepo.findByPeriodoId(any())).thenReturn(List.of(cuotaImpaga("a@x.com", ayer)));
        RecordatorioCuota rec = new RecordatorioCuota();
        rec.setVecinoEmail("a@x.com");
        rec.setNivelNotificado(3);
        when(recordatorioRepo.findByPeriodoIdAndVecinoEmail(any(), eq("a@x.com")))
                .thenReturn(Optional.of(rec));

        scheduler.revisar();

        verify(publisher, never()).publicar(any(), any());
        verify(recordatorioRepo).save(rec);
    }
}
