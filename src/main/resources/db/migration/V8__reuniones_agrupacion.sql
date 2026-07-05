-- ============================================================================
-- V8: Reuniones periódicas de agrupaciones.
--   Reunión semanal (día + hora), pausa por vacaciones (rango) y
--   cancelación de fechas puntuales.
-- ============================================================================
ALTER TABLE agrupaciones ADD COLUMN IF NOT EXISTS reunion_dia_semana INTEGER; -- 1=Lunes .. 7=Domingo (ISO)
ALTER TABLE agrupaciones ADD COLUMN IF NOT EXISTS reunion_hora TIME;
ALTER TABLE agrupaciones ADD COLUMN IF NOT EXISTS pausa_inicio DATE;
ALTER TABLE agrupaciones ADD COLUMN IF NOT EXISTS pausa_fin DATE;

CREATE TABLE IF NOT EXISTS reuniones_canceladas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agrupacion_id UUID NOT NULL,
    fecha DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reunion_cancelada UNIQUE (agrupacion_id, fecha)
);
