package com.portalcomunitario.mscommunity.cuota;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecordatorioCuotaRepository extends JpaRepository<RecordatorioCuota, UUID> {
    Optional<RecordatorioCuota> findByPeriodoIdAndVecinoEmail(UUID periodoId, String vecinoEmail);
}
