-- Nombre del autor del evento (para mostrar sin depender de ms-auth).
ALTER TABLE events ADD COLUMN IF NOT EXISTS author_nombre VARCHAR(120);
