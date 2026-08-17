-- El formulario de solicitud de acceso pasa de 5 campos a un cuestionario completo.
--
-- Las columnas que ya existían se renombran en vez de recrearse, para no perder las
-- solicitudes históricas:
--   nombre_completo -> nombre      (las viejas no tienen apellidos por separado)
--   pais            -> pais_nacimiento
--   universidad     -> institucion
--   uso             -> motivacion  ("¿Por qué le interesa el Modelo PipoE?")
--
-- Las columnas nuevas quedan NULL: una solicitud vieja genuinamente no tiene ese dato, y
-- rellenarlo con un valor inventado sería peor que dejarlo vacío. La obligatoriedad se exige
-- en RegisterSolicitudAccesoRequest, que es lo que aplica a las solicitudes nuevas.

ALTER TABLE solicitudes_acceso RENAME COLUMN nombre_completo TO nombre;
ALTER TABLE solicitudes_acceso RENAME COLUMN pais TO pais_nacimiento;
ALTER TABLE solicitudes_acceso RENAME COLUMN universidad TO institucion;
ALTER TABLE solicitudes_acceso RENAME COLUMN uso TO motivacion;

-- "¿Por qué le interesa?" es texto libre y 500 quedaba corto.
ALTER TABLE solicitudes_acceso ALTER COLUMN motivacion TYPE VARCHAR(1000);
ALTER TABLE solicitudes_acceso ALTER COLUMN institucion TYPE VARCHAR(200);

ALTER TABLE solicitudes_acceso ADD COLUMN apellidos          VARCHAR(150);
ALTER TABLE solicitudes_acceso ADD COLUMN nivel_instruccion  VARCHAR(30);
ALTER TABLE solicitudes_acceso ADD COLUMN genero             VARCHAR(20);
ALTER TABLE solicitudes_acceso ADD COLUMN rango_edad         VARCHAR(20);
ALTER TABLE solicitudes_acceso ADD COLUMN ocupacion          VARCHAR(40);
ALTER TABLE solicitudes_acceso ADD COLUMN ocupacion_otra     VARCHAR(150);
-- Sólo se completa si es distinto al de nacimiento.
ALTER TABLE solicitudes_acceso ADD COLUMN pais_residencia    VARCHAR(100);
ALTER TABLE solicitudes_acceso ADD COLUMN usos_otro          VARCHAR(150);
ALTER TABLE solicitudes_acceso ADD COLUMN canal_otro         VARCHAR(150);

-- Los usos previstos y los canales por los que se enteró admiten varias opciones cada uno,
-- así que van en tablas hijas en lugar de una lista serializada en una columna.
CREATE TABLE solicitud_usos (
    solicitud_acceso_id BIGINT      NOT NULL REFERENCES solicitudes_acceso (solicitud_acceso_id) ON DELETE CASCADE,
    uso                 VARCHAR(40) NOT NULL,
    PRIMARY KEY (solicitud_acceso_id, uso)
);

CREATE TABLE solicitud_canales (
    solicitud_acceso_id BIGINT      NOT NULL REFERENCES solicitudes_acceso (solicitud_acceso_id) ON DELETE CASCADE,
    canal               VARCHAR(40) NOT NULL,
    PRIMARY KEY (solicitud_acceso_id, canal)
);
