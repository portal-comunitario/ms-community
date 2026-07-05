package com.portalcomunitario.mscommunity.agrupacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InscripcionAgrupacionRepository extends JpaRepository<InscripcionAgrupacion, UUID> {
    List<InscripcionAgrupacion> findByVecinoEmail(String vecinoEmail);
    List<InscripcionAgrupacion> findByAgrupacionId(UUID agrupacionId);
    long countByAgrupacionId(UUID agrupacionId);
    void deleteByAgrupacionIdAndVecinoEmail(UUID agrupacionId, String vecinoEmail);
    boolean existsByAgrupacionIdAndVecinoEmail(UUID agrupacionId, String vecinoEmail);
}
