package com.portalcomunitario.mscommunity.asistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {
    List<Asistencia> findByEventoId(UUID eventoId);
    List<Asistencia> findByAgrupacionIdAndVecinoEmail(UUID agrupacionId, String vecinoEmail);
    Optional<Asistencia> findByEventoIdAndVecinoEmail(UUID eventoId, String vecinoEmail);
}
