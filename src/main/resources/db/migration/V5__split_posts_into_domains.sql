-- ============================================================================
-- V5: Separación del monolítico "posts" en los 3 dominios de contenido
--     (Comunicados, Tablón/Avisos, Eventos). Ver DISEÑO-PORTAL.md §2.
--     La tabla "posts" se conserva por ahora para no romper el frontend
--     hasta la Fase B; quedará obsoleta y se eliminará luego.
-- ============================================================================

-- Dominio A — Comunicados / Noticias (informativo, sin moderación)
CREATE TABLE IF NOT EXISTS comunicados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    categoria VARCHAR(20) NOT NULL DEFAULT 'NOTICIA',
    imagen_url VARCHAR(500),
    author_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dominio B — Tablón Vecinal / Avisos (marketplace, con moderación)
CREATE TABLE IF NOT EXISTS avisos_tablon (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT NOT NULL,
    categoria VARCHAR(30) NOT NULL DEFAULT 'SERVICIO',
    author_email VARCHAR(255) NOT NULL,
    latitud DOUBLE PRECISION,
    longitud DOUBLE PRECISION,
    direccion VARCHAR(500),
    precio INTEGER,
    contacto VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    resuelto BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dominio C — Eventos: añadir categoría (entidad única, ya existente)
ALTER TABLE events ADD COLUMN IF NOT EXISTS categoria VARCHAR(30) NOT NULL DEFAULT 'GENERAL';

-- ---------------------------------------------------------------------------
-- Migración de datos existentes desde "posts" según su tipo
-- ---------------------------------------------------------------------------

-- ANUNCIO/NOTICIA -> comunicados
INSERT INTO comunicados (id, titulo, contenido, categoria, author_email, created_at)
SELECT id, titulo, contenido,
       CASE WHEN tipo = 'NOTICIA' THEN 'NOTICIA' ELSE 'AVISO' END,
       author_email, created_at
FROM posts
WHERE tipo IN ('ANUNCIO', 'NOTICIA')
ON CONFLICT (id) DO NOTHING;

-- SERVICIO/COMPRA_VENTA/ARRIENDO/PERDIDO_ENCONTRADO -> avisos_tablon
INSERT INTO avisos_tablon (id, titulo, descripcion, categoria, author_email, latitud, longitud, direccion, estado, resuelto, created_at)
SELECT id, titulo, contenido, tipo, author_email, latitud, longitud, direccion,
       COALESCE(estado, 'PENDIENTE'), FALSE, created_at
FROM posts
WHERE tipo IN ('SERVICIO', 'COMPRA_VENTA', 'ARRIENDO', 'PERDIDO_ENCONTRADO')
ON CONFLICT (id) DO NOTHING;

-- EVENTO (posts) -> events (usa created_at como fecha_inicio placeholder)
INSERT INTO events (id, titulo, descripcion, fecha_inicio, ubicacion, categoria, author_email, created_at)
SELECT id, titulo, contenido, created_at, direccion, 'GENERAL', author_email, created_at
FROM posts
WHERE tipo = 'EVENTO'
ON CONFLICT (id) DO NOTHING;
