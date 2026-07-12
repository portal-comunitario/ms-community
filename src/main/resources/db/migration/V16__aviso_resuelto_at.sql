-- Fecha en que un aviso se marcó como resuelto. Se usa para el borrado
-- automático a los 30 días en "modo fantasma" (visible solo para su autor).
ALTER TABLE avisos_tablon ADD COLUMN resuelto_at TIMESTAMP;
