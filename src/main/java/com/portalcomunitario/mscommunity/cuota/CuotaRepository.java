package com.portalcomunitario.mscommunity.cuota;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CuotaRepository extends JpaRepository<Cuota, UUID> {
    List<Cuota> findByAgrupacionIdOrderByVencimientoAsc(UUID agrupacionId);
    List<Cuota> findByAgrupacionIdAndVecinoEmailOrderByVencimientoAsc(UUID agrupacionId, String vecinoEmail);
    List<Cuota> findByPeriodoId(UUID periodoId);
}
