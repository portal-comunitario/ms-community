package com.portalcomunitario.mscommunity.comunicado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComunicadoRepository extends JpaRepository<Comunicado, UUID> {
    List<Comunicado> findAllByOrderByCreatedAtDesc();
}
