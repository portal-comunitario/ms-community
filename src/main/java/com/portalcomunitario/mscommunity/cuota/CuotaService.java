package com.portalcomunitario.mscommunity.cuota;

import com.portalcomunitario.mscommunity.agrupacion.InscripcionAgrupacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CuotaService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "CL"));

    private final CuotaPeriodoRepository periodoRepo;
    private final CuotaRepository cuotaRepo;
    private final InscripcionAgrupacionRepository inscripcionRepo;

    public CuotaService(CuotaPeriodoRepository periodoRepo, CuotaRepository cuotaRepo,
                        InscripcionAgrupacionRepository inscripcionRepo) {
        this.periodoRepo = periodoRepo;
        this.cuotaRepo = cuotaRepo;
        this.inscripcionRepo = inscripcionRepo;
    }

    public CuotaPeriodo periodoActivo(UUID agrupacionId) {
        return periodoRepo.findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(agrupacionId, EstadoPeriodo.ABIERTA)
                .orElse(null);
    }

    @Transactional
    public CuotaPeriodo activar(UUID agrupacionId, CuotaRequest req) {
        if (periodoActivo(agrupacionId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay un período de cuotas abierto en esta agrupación");
        }
        if (req.monto() == null || req.monto() <= 0 || req.periodicidad() == null
                || req.fechaInicio() == null || req.fechaFin() == null || req.fechaFin().isBefore(req.fechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos del período inválidos");
        }
        CuotaPeriodo p = new CuotaPeriodo();
        p.setAgrupacionId(agrupacionId);
        p.setMonto(req.monto());
        p.setPeriodicidad(parsePeriodicidad(req.periodicidad()));
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        p.setEstado(EstadoPeriodo.ABIERTA);
        p = periodoRepo.save(p);

        for (String email : sociosDe(agrupacionId)) {
            generarCuotas(p, email, p.getFechaInicio());
        }
        return p;
    }

    @Transactional
    public CuotaPeriodo cerrar(UUID agrupacionId) {
        CuotaPeriodo p = periodoActivo(agrupacionId);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay período de cuotas abierto");
        p.setEstado(EstadoPeriodo.CERRADA);
        return periodoRepo.save(p);
        // NOTA: el aviso de cierre a los socios se enviará vía ms-notifications (Fase E).
    }

    /** Corrige el monto del período abierto y lo propaga a las cuotas aún NO pagadas. */
    @Transactional
    public CuotaPeriodo actualizarMonto(UUID agrupacionId, Integer nuevoMonto) {
        CuotaPeriodo p = periodoActivo(agrupacionId);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay período de cuotas abierto");
        if (nuevoMonto == null || nuevoMonto <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a 0");
        }
        p.setMonto(nuevoMonto);
        periodoRepo.save(p);
        for (Cuota c : cuotaRepo.findByAgrupacionIdOrderByVencimientoAsc(agrupacionId)) {
            if (p.getId().equals(c.getPeriodoId()) && !c.isPagada()) {
                c.setMonto(nuevoMonto);
                cuotaRepo.save(c);
            }
        }
        return p;
    }

    /** Al inscribirse un socio nuevo, generar sus cuotas desde hoy hasta el fin del período abierto. */
    @Transactional
    public void generarParaNuevoSocio(UUID agrupacionId, String email) {
        CuotaPeriodo p = periodoActivo(agrupacionId);
        if (p == null) return;
        if (!cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(agrupacionId, email).isEmpty()) return;
        LocalDate desde = LocalDate.now().isAfter(p.getFechaInicio()) ? LocalDate.now() : p.getFechaInicio();
        generarCuotas(p, email, desde);
    }

    private void generarCuotas(CuotaPeriodo p, String email, LocalDate desde) {
        if (p.getPeriodicidad() == Periodicidad.SEMANAL) {
            LocalDate cursor = desde;
            while (!cursor.isAfter(p.getFechaFin())) {
                LocalDate venc = cursor.plusDays(6);
                if (venc.isAfter(p.getFechaFin())) venc = p.getFechaFin();
                crearCuota(p, email, "Semana del " + cursor.format(DIA), venc);
                cursor = cursor.plusDays(7);
            }
        } else {
            LocalDate mes = desde.withDayOfMonth(1);
            while (!mes.isAfter(p.getFechaFin())) {
                LocalDate finMes = mes.withDayOfMonth(mes.lengthOfMonth());
                LocalDate venc = finMes.isAfter(p.getFechaFin()) ? p.getFechaFin() : finMes;
                if (!venc.isBefore(p.getFechaInicio())) {
                    crearCuota(p, email, capitalizar(mes.format(MES)), venc);
                }
                mes = mes.plusMonths(1);
            }
        }
    }

    private void crearCuota(CuotaPeriodo p, String email, String etiqueta, LocalDate venc) {
        Cuota c = new Cuota();
        c.setPeriodoId(p.getId());
        c.setAgrupacionId(p.getAgrupacionId());
        c.setVecinoEmail(email);
        c.setEtiqueta(etiqueta);
        c.setMonto(p.getMonto());
        c.setVencimiento(venc);
        c.setPagada(false);
        cuotaRepo.save(c);
    }

    public List<Cuota> deAgrupacion(UUID agrupacionId) {
        return cuotaRepo.findByAgrupacionIdOrderByVencimientoAsc(agrupacionId);
    }

    public List<Cuota> misCuotas(UUID agrupacionId, String email) {
        return cuotaRepo.findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(agrupacionId, email);
    }

    public Cuota marcarPago(UUID cuotaId, boolean pagada) {
        Cuota c = cuotaRepo.findById(cuotaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuota no encontrada"));
        c.setPagada(pagada);
        c.setFechaPago(pagada ? LocalDateTime.now() : null);
        return cuotaRepo.save(c);
    }

    private List<String> sociosDe(UUID agrupacionId) {
        return inscripcionRepo.findByAgrupacionId(agrupacionId).stream()
                .map(i -> i.getVecinoEmail())
                .toList();
    }

    private Periodicidad parsePeriodicidad(String v) {
        try {
            return Periodicidad.valueOf(v.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periodicidad inválida");
        }
    }

    private String capitalizar(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
