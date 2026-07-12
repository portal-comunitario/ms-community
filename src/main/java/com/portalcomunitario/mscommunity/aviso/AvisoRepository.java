package com.portalcomunitario.mscommunity.aviso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AvisoRepository extends JpaRepository<Aviso, UUID> {
    List<Aviso> findAllByOrderByCreatedAtDesc();
    List<Aviso> findByEstadoOrderByCreatedAtDesc(AvisoEstado estado);

    /** Borra los avisos resueltos cuya marca de resolución sea anterior al corte (30 días). */
    @Modifying
    @Transactional
    @Query("DELETE FROM Aviso a WHERE a.resuelto = true AND a.resueltoAt < :corte")
    int deleteResueltosAnterioresA(@Param("corte") LocalDateTime corte);
}
