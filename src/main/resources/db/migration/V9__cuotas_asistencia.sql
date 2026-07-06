-- ============================================================================
-- V9: Objetivo 4 — Cuotas (por período, por agrupación) y Asistencia a actividades.
-- ============================================================================
CREATE TABLE IF NOT EXISTS cuota_periodos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agrupacion_id UUID NOT NULL,
    monto INTEGER NOT NULL,
    periodicidad VARCHAR(20) NOT NULL,           -- SEMANAL / MENSUAL
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA', -- ABIERTA / CERRADA
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    periodo_id UUID NOT NULL,
    agrupacion_id UUID NOT NULL,
    vecino_email VARCHAR(255) NOT NULL,
    etiqueta VARCHAR(80) NOT NULL,
    monto INTEGER NOT NULL,
    vencimiento DATE NOT NULL,
    pagada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_pago TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cuotas_agr_email ON cuotas(agrupacion_id, vecino_email);

CREATE TABLE IF NOT EXISTS asistencias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id UUID NOT NULL,
    agrupacion_id UUID NOT NULL,
    vecino_email VARCHAR(255) NOT NULL,
    presente BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_asistencia UNIQUE (evento_id, vecino_email)
);
