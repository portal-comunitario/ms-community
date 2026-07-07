-- Difusión de eventos a la comunidad: marca de notificado y de recordatorio enviado.
ALTER TABLE events ADD COLUMN IF NOT EXISTS notificado_comunidad TIMESTAMP;
ALTER TABLE events ADD COLUMN IF NOT EXISTS recordatorio_enviado BOOLEAN NOT NULL DEFAULT FALSE;
