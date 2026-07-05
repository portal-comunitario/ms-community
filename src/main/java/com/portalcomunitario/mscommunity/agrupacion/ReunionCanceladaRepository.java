package com.portalcomunitario.mscommunity.agrupacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReunionCanceladaRepository extends JpaRepository<ReunionCancelada, UUID> {
    List<ReunionCancelada> findByAgrupacionId(UUID agrupacionId);
    boolean existsByAgrupacionIdAndFecha(UUID agrupacionId, LocalDate fecha);
    void deleteByAgrupacionIdAndFecha(UUID agrupacionId, LocalDate fecha);
}
