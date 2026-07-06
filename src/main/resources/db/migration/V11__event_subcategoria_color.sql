-- Tipo libre del evento (subcategoría opcional) y color del pin en el mapa.
ALTER TABLE events ADD COLUMN IF NOT EXISTS subcategoria VARCHAR(60);
ALTER TABLE events ADD COLUMN IF NOT EXISTS color VARCHAR(9);
