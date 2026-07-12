package com.portalcomunitario.mscommunity.asistencia;

import java.time.LocalDate;
import java.util.UUID;

/** Una ocurrencia de la reunión periódica sobre la que se pasa lista. */
public record SesionAsistenciaDto(UUID sesionId, LocalDate fecha, String hora) {}
