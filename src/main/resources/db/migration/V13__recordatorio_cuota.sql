-- Control de idempotencia de recordatorios de cuota pendiente ("nivel de deuda").
CREATE TABLE IF NOT EXISTS recordatorio_cuota (
    id UUID PRIMARY KEY,
    periodo_id UUID NOT NULL,
    vecino_email VARCHAR(255) NOT NULL,
    nivel_notificado INTEGER NOT NULL DEFAULT 0,
    ultima_fecha DATE,
    CONSTRAINT uq_recordatorio_periodo_vecino UNIQUE (periodo_id, vecino_email)
);
