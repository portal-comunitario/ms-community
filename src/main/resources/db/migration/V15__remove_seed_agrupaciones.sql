-- Las agrupaciones NO deben venir precargadas: cada comunidad arma las suyas.
-- (V6 sembraba "Club de Adulto Mayor" y "Centro de Madres" en cada schema.)
-- Se eliminan solo los registros sembrados intactos: mismo nombre/descripción y
-- sin socios ni actividades asociadas. Las que una comunidad ya usó se conservan.
-- Excepción: Villa Las Flores conserva sus agrupaciones (comunidad real existente).
DELETE FROM agrupaciones a
 WHERE current_schema() <> 'villa_las_flores'
   AND a.nombre IN ('Club de Adulto Mayor', 'Centro de Madres')
   AND a.descripcion IN (
        'Actividades, talleres y encuentros para las personas mayores de la comunidad.',
        'Talleres, reuniones y actividades del centro de madres.')
   AND NOT EXISTS (SELECT 1 FROM inscripciones_agrupacion i WHERE i.agrupacion_id = a.id)
   AND NOT EXISTS (SELECT 1 FROM events e WHERE e.agrupacion_id = a.id);
