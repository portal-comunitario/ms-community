-- ============================================================================
-- V6: Agrupaciones (clubes/organizaciones funcionales) + socios.
--   Las actividades de una agrupación son eventos con agrupacion_id.
-- ============================================================================
CREATE TABLE IF NOT EXISTS agrupaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    tipo VARCHAR(30) NOT NULL DEFAULT 'OTRO',
    responsable VARCHAR(255),
    maneja_cuotas BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inscripciones_agrupacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agrupacion_id UUID NOT NULL,
    vecino_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inscripcion UNIQUE (agrupacion_id, vecino_email)
);

ALTER TABLE events ADD COLUMN IF NOT EXISTS agrupacion_id UUID;

-- Precargar las dos agrupaciones típicas
INSERT INTO agrupaciones (nombre, descripcion, tipo)
SELECT 'Club de Adulto Mayor', 'Actividades, talleres y encuentros para las personas mayores de la comunidad.', 'ADULTO_MAYOR'
WHERE NOT EXISTS (SELECT 1 FROM agrupaciones WHERE tipo = 'ADULTO_MAYOR');

INSERT INTO agrupaciones (nombre, descripcion, tipo)
SELECT 'Centro de Madres', 'Talleres, reuniones y actividades del centro de madres.', 'CENTRO_DE_MADRES'
WHERE NOT EXISTS (SELECT 1 FROM agrupaciones WHERE tipo = 'CENTRO_DE_MADRES');
