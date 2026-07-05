package com.portalcomunitario.mscommunity.agrupacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgrupacionRepository extends JpaRepository<Agrupacion, UUID> {
    List<Agrupacion> findAllByOrderByNombreAsc();
}
