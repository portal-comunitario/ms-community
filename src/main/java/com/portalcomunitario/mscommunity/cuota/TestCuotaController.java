package com.portalcomunitario.mscommunity.cuota;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint de prueba (dev): ejecuta la revisión de cuotas pendientes sin esperar el cron. */
@RestController
@RequestMapping("/test/cuotas")
public class TestCuotaController {

    private final CuotaRecordatorioScheduler scheduler;

    public TestCuotaController(CuotaRecordatorioScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/revisar")
    public String revisar() {
        scheduler.revisar();
        return "Revisión de cuotas pendientes ejecutada (mira los logs)";
    }
}
