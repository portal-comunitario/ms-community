-- ============================================================================
-- V7: Las agrupaciones son genéricas — se elimina la categoría fija "tipo".
--   Se pueden crear agrupaciones de cualquier índole solo con nombre/descripción.
-- ============================================================================
ALTER TABLE agrupaciones DROP COLUMN IF EXISTS tipo;
