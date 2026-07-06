-- Ubicación geográfica del evento (pin en el mapa)
ALTER TABLE events ADD COLUMN IF NOT EXISTS latitud DOUBLE PRECISION;
ALTER TABLE events ADD COLUMN IF NOT EXISTS longitud DOUBLE PRECISION;

-- Recurrencia estilo calendario (regla, sin instancias materializadas)
ALTER TABLE events ADD COLUMN IF NOT EXISTS recurrente BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(20);   -- DIARIA | SEMANAL | MENSUAL | ANUAL
ALTER TABLE events ADD COLUMN IF NOT EXISTS intervalo INTEGER;        -- cada N (frecuencia); 1 por defecto
ALTER TABLE events ADD COLUMN IF NOT EXISTS recurrencia_fin DATE;     -- hasta (inclusive); null = sin término
