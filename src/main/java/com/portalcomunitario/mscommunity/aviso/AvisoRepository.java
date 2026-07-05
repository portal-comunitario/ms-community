package com.portalcomunitario.mscommunity.aviso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvisoRepository extends JpaRepository<Aviso, UUID> {
    List<Aviso> findAllByOrderByCreatedAtDesc();
    List<Aviso> findByEstadoOrderByCreatedAtDesc(AvisoEstado estado);
}
