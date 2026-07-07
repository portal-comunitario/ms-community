package com.portalcomunitario.mscommunity.cuota;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuotaPeriodoRepository extends JpaRepository<CuotaPeriodo, UUID> {
    Optional<CuotaPeriodo> findFirstByAgrupacionIdAndEstadoOrderByCreatedAtDesc(UUID agrupacionId, EstadoPeriodo estado);
    List<CuotaPeriodo> findByAgrupacionIdOrderByCreatedAtDesc(UUID agrupacionId);
    List<CuotaPeriodo> findByEstado(EstadoPeriodo estado);
}
