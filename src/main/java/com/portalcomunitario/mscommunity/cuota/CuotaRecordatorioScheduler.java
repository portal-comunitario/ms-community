package com.portalcomunitario.mscommunity.cuota;

import com.portalcomunitario.mscommunity.agrupacion.AgrupacionRepository;
import com.portalcomunitario.mscommunity.contacto.ContactoClient;
import com.portalcomunitario.mscommunity.messaging.Destinatario;
import com.portalcomunitario.mscommunity.messaging.NotificacionEvento;
import com.portalcomunitario.mscommunity.messaging.NotificacionPublisher;
import com.portalcomunitario.mscommunity.messaging.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Revisa diariamente las cuotas pendientes por socio y publica un recordatorio
 * cuando la deuda alcanza un nuevo nivel (≥2 pendientes). Regla "nivel de deuda":
 * pendiente = cuota impaga cuyo vencimiento es hoy o antes, o vence dentro de 1 día
 * (da gracia a la primera). Solo se notifica si el nivel supera el ya notificado;
 * si el socio paga y baja, el nivel se re-arma. Sin recordatorios por tiempo.
 */
@Component
public class CuotaRecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(CuotaRecordatorioScheduler.class);
    private static final int UMBRAL = 2;

    private final CuotaPeriodoRepository periodoRepo;
    private final CuotaRepository cuotaRepo;
    private final RecordatorioCuotaRepository recordatorioRepo;
    private final AgrupacionRepository agrupacionRepo;
    private final ContactoClient contactoClient;
    private final NotificacionPublisher publisher;

    public CuotaRecordatorioScheduler(CuotaPeriodoRepository periodoRepo,
                                      CuotaRepository cuotaRepo,
                                      RecordatorioCuotaRepository recordatorioRepo,
                                      AgrupacionRepository agrupacionRepo,
                                      ContactoClient contactoClient,
                                      NotificacionPublisher publisher) {
        this.periodoRepo = periodoRepo;
        this.cuotaRepo = cuotaRepo;
        this.recordatorioRepo = recordatorioRepo;
        this.agrupacionRepo = agrupacionRepo;
        this.contactoClient = contactoClient;
        this.publisher = publisher;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void revisar() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(1); // vencida o vence dentro de 1 día
        int publicados = 0;

        for (CuotaPeriodo periodo : periodoRepo.findByEstado(EstadoPeriodo.ABIERTA)) {
            Map<String, List<Cuota>> porSocio = new LinkedHashMap<>();
            for (Cuota c : cuotaRepo.findByPeriodoId(periodo.getId())) {
                porSocio.computeIfAbsent(c.getVecinoEmail(), k -> new ArrayList<>()).add(c);
            }

            for (Map.Entry<String, List<Cuota>> e : porSocio.entrySet()) {
                String email = e.getKey();
                List<Cuota> pendientes = e.getValue().stream()
                        .filter(c -> !c.isPagada() && !c.getVencimiento().isAfter(limite))
                        .sorted((a, b) -> a.getVencimiento().compareTo(b.getVencimiento()))
                        .toList();
                int nivel = pendientes.size();

                RecordatorioCuota rec = recordatorioRepo
                        .findByPeriodoIdAndVecinoEmail(periodo.getId(), email)
                        .orElseGet(() -> {
                            RecordatorioCuota r = new RecordatorioCuota();
                            r.setPeriodoId(periodo.getId());
                            r.setVecinoEmail(email);
                            r.setNivelNotificado(0);
                            return r;
                        });

                if (nivel >= UMBRAL && nivel > rec.getNivelNotificado()) {
                    if (publicar(periodo, email, pendientes)) {
                        rec.setNivelNotificado(nivel);
                        rec.setUltimaFecha(hoy);
                        recordatorioRepo.save(rec);
                        publicados++;
                    }
                } else if (nivel < rec.getNivelNotificado()) {
                    // Pagó algunas: re-armar el nivel para futuros aumentos.
                    rec.setNivelNotificado(nivel);
                    recordatorioRepo.save(rec);
                }
            }
        }
        log.info("Revisión de cuotas pendientes: {} recordatorio(s) publicado(s)", publicados);
    }

    private boolean publicar(CuotaPeriodo periodo, String email, List<Cuota> pendientes) {
        ContactoClient.Contacto contacto = contactoClient.porEmail(email);
        if (contacto == null) {
            log.warn("Sin contacto para {}, se omite recordatorio de cuota", email);
            return false;
        }
        String agrupacion = agrupacionRepo.findById(periodo.getAgrupacionId())
                .map(a -> a.getNombre()).orElse("tu agrupación");

        int total = pendientes.stream().mapToInt(Cuota::getMonto).sum();
        StringBuilder detalle = new StringBuilder();
        for (Cuota c : pendientes) {
            detalle.append("\n• ").append(c.getEtiqueta()).append(" ($").append(c.getMonto()).append(")");
        }
        String mensaje = "Hola " + contacto.nombre() + ", tienes " + pendientes.size()
                + " cuota(s) pendiente(s) en " + agrupacion + ":" + detalle
                + "\nTotal adeudado: $" + total + ". Regulariza en el portal.";

        Destinatario dest = new Destinatario(contacto.nombre(), contacto.email(),
                contacto.telefono(), contacto.notificacionesActivas());
        NotificacionEvento evento = new NotificacionEvento(
                "CUOTA_PENDIENTE",
                "Tienes cuotas pendientes en " + agrupacion,
                mensaje,
                List.of(dest));
        publisher.publicar(RabbitConfig.RK_CUOTA_PENDIENTE, evento);
        return true;
    }
}
