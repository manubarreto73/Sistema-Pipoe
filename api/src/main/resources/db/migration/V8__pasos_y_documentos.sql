-- Modelo PipoE dentro del proyecto: las 5 fases, sus pasos y el documento de cada paso.

-- ---------------------------------------------------------------------------
-- 1. Las fases pasan a identificarse por nombre y no por número.
--    El entero 1..5 no decía nada al leer la base ni al depurar una respuesta;
--    el orden de las fases sigue viviendo en el enum Fase de Java.
-- ---------------------------------------------------------------------------
ALTER TABLE colaborador_permisos ADD COLUMN fase_nombre VARCHAR(20);

UPDATE colaborador_permisos SET fase_nombre = CASE fase
    WHEN 1 THEN 'PROMOCION'
    WHEN 2 THEN 'INDAGACION'
    WHEN 3 THEN 'PROGRAMACION'
    WHEN 4 THEN 'ORGANIZACION'
    WHEN 5 THEN 'EVALUACION'
END;

-- Al borrar la columna se va con ella uk_permiso_colaborador_fase, que la incluía.
ALTER TABLE colaborador_permisos DROP COLUMN fase;
ALTER TABLE colaborador_permisos RENAME COLUMN fase_nombre TO fase;
ALTER TABLE colaborador_permisos ALTER COLUMN fase SET NOT NULL;
ALTER TABLE colaborador_permisos
    ADD CONSTRAINT uk_permiso_colaborador_fase UNIQUE (colaborador_id, fase);

-- ---------------------------------------------------------------------------
-- 2. Catálogo de pasos. Es el mismo para todos los proyectos: acá no hay nada
--    de ningún proyecto en particular, sólo el modelo PipoE.
--
--    El "producto" de cada fase (Plan de promoción, Diagnóstico situacional...)
--    es un paso más, marcado con es_producto y con orden 99 para que quede
--    siempre al final. Así su documento se guarda, versiona y permisa igual que
--    el de cualquier otro paso, sin duplicar la mitad del dominio.
--
--    titulo es el texto tal cual el modelo; titulo_corto es la etiqueta que se
--    muestra en el diagrama de flujo, donde el título entero no entra.
--
--    explicacion y ejemplo arrancan vacíos: los carga la dueña desde la app.
-- ---------------------------------------------------------------------------
CREATE TABLE pasos (
    paso_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fase         VARCHAR(20) NOT NULL,
    orden        INTEGER     NOT NULL,
    titulo       TEXT        NOT NULL,
    titulo_corto VARCHAR(60) NOT NULL,
    explicacion  TEXT        NOT NULL DEFAULT '',
    ejemplo      TEXT        NOT NULL DEFAULT '',
    es_producto  BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_paso_fase_orden UNIQUE (fase, orden)
);

-- Los títulos van con las erratas del archivo original corregidas:
--   "desintereasadas" -> "desinteresadas"      (Promoción 7)
--   "frente a a las"  -> "frente a las"        (Indagación 5)
--   "probblema"       -> "problema"            (Indagación 6)
--   "rqueridos"       -> "requeridos"          (Organización 5)
--   "distintos momento" -> "distintos momentos" (Evaluación 7)

INSERT INTO pasos (fase, orden, titulo_corto, titulo, es_producto) VALUES
('PROMOCION', 1, 'Partes interesadas',
 'Identificar partes interesadas ante el problema objeto de atención', FALSE),
('PROMOCION', 2, 'Partes desinteresadas',
 'Identificar partes desinteresadas ante el problema objeto de atención', FALSE),
('PROMOCION', 3, 'Intereses y expectativas',
 'Identificar intereses y expectativas de partes interesadas y desinteresadas ante el problema objeto de atención', FALSE),
('PROMOCION', 4, 'Posición frente a la actuación',
 'Identificar la posición de las partes interesadas y desinteresadas frente a la actuación ante el problema objeto de atención', FALSE),
('PROMOCION', 5, 'Capacidad de influencia',
 'Identificar la capacidad de influencia de las partes interesadas y desinteresadas frente a la actuación ante el problema objeto de atención', FALSE),
('PROMOCION', 6, 'Alianzas posibles',
 'Identificar posibles alianzas estratégicas y contingentes frente a la actuación ante el problema objeto de atención', FALSE),
('PROMOCION', 7, 'Estrategias de vinculación',
 'Identificar estrategias de vinculación, negociación y comunicación con partes interesadas y desinteresadas', FALSE),
('PROMOCION', 8, 'Base social de apoyo',
 'Identificar base social de apoyo en respaldo a la actuación ante el problema objeto de atención', FALSE),
('PROMOCION', 99, 'Plan de promoción', 'Plan de promoción', TRUE),

('INDAGACION', 1, 'Conceptualización del problema',
 'Conceptualizar el problema objeto de atención', FALSE),
('INDAGACION', 2, 'Espacio de actuación',
 'Configurar el espacio de actuación ante el problema objeto de atención', FALSE),
('INDAGACION', 3, 'Actores afectados',
 'Caracterizar a los actores directamente afectados por el problema objeto de atención', FALSE),
('INDAGACION', 4, 'Respuestas previas y en curso',
 'Valorar respuestas de actuación en curso y anteriores ante el problema objeto de atención', FALSE),
('INDAGACION', 5, 'Actuación de los afectados',
 'Valorar la actuación de los actores afectados frente a las respuestas ante el problema objeto de atención', FALSE),
('INDAGACION', 6, 'Actuación de los involucrados',
 'Valorar la actuación de los actores involucrados frente a las respuestas ante el problema objeto de atención', FALSE),
('INDAGACION', 99, 'Diagnóstico situacional', 'Diagnóstico situacional', TRUE),

('PROGRAMACION', 1, 'Objetivos',
 'Delimitar los objetivos de la intervención', FALSE),
('PROGRAMACION', 2, 'Actividades',
 'Establecer las actividades requeridas para el logro de los objetivos delimitados', FALSE),
('PROGRAMACION', 3, 'Resultados esperados',
 'Definir los resultados esperados de la intervención', FALSE),
('PROGRAMACION', 4, 'Recursos',
 'Identificar los recursos (en el sentido amplio de la palabra) requeridos para la ejecución de las actividades, la obtención de resultados y el logro de los objetivos', FALSE),
('PROGRAMACION', 5, 'Presupuesto', 'Elaborar el presupuesto', FALSE),
('PROGRAMACION', 6, 'Cronograma', 'Elaborar el cronograma', FALSE),
('PROGRAMACION', 99, 'Plan de acción', 'Plan de acción', TRUE),

('ORGANIZACION', 1, 'Responsabilidades',
 'Definir responsabilidades de ejecución de las actividades para la obtención de los resultados y el logro de los objetivos', FALSE),
('ORGANIZACION', 2, 'Funciones',
 'Asignar funciones requeridas para la ejecución de las actividades, la obtención de resultados y el logro de los objetivos', FALSE),
('ORGANIZACION', 3, 'Coordinación',
 'Establecer los mecanismos de coordinación entre las distintas personas responsables de las actividades y las instancias involucradas en la ejecución de las mismas', FALSE),
('ORGANIZACION', 4, 'Comunicación',
 'Establecer los mecanismos de comunicación entre las distintas personas e instancias involucradas en la ejecución', FALSE),
('ORGANIZACION', 5, 'Verificación de recursos',
 'Establecer los mecanismos de verificación para garantizar la disponibilidad efectiva de los recursos requeridos para la ejecución de las actividades', FALSE),
('ORGANIZACION', 99, 'Plan de organización', 'Plan de organización', TRUE),

('EVALUACION', 1, 'Parámetros de evaluación',
 'Establecer los parámetros de orientación conceptual y metodológica de evaluación', FALSE),
('EVALUACION', 2, 'Criterios e indicadores',
 'Identificar los criterios, indicadores, sistemas de indicadores o estándares de evaluación que serán utilizados en los distintos momentos de la evaluación', FALSE),
('EVALUACION', 3, 'Fuentes de información',
 'Seleccionar las fuentes de información y el tipo de información requerida de cada una de las fuentes seleccionadas', FALSE),
('EVALUACION', 4, 'Ubicación temporal',
 'Definir la ubicación temporal de las actividades de evaluación', FALSE),
('EVALUACION', 5, 'Técnicas de indagación',
 'Seleccionar las técnicas de indagación a ser utilizadas en los distintos momentos de la evaluación', FALSE),
('EVALUACION', 6, 'Instrumentos',
 'Diseñar los instrumentos a ser aplicados en los distintos momentos de la evaluación', FALSE),
('EVALUACION', 7, 'Participación',
 'Definir los tipos y responsabilidades de participación en los distintos momentos de la evaluación', FALSE),
('EVALUACION', 99, 'Diseño de evaluación', 'Diseño de evaluación', TRUE);

-- ---------------------------------------------------------------------------
-- 3. El documento de cada paso, por proyecto.
--
--    contenido es TEXT: en Postgres no tiene límite práctico (~1 GB) y se lee
--    y respalda como cualquier otra columna. Deliberadamente NO es un large
--    object (@Lob), que vive fuera de la tabla y complica backups y borrados.
--
--    version es para el bloqueo optimista: si dos personas guardan sobre la
--    misma base, la segunda recibe 409 en vez de pisar lo de la primera.
-- ---------------------------------------------------------------------------
CREATE TABLE documentos (
    documento_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proyecto_id     BIGINT      NOT NULL REFERENCES proyectos (proyecto_id) ON DELETE CASCADE,
    paso_id         BIGINT      NOT NULL REFERENCES pasos (paso_id),
    contenido       TEXT        NOT NULL DEFAULT '',
    completado      BOOLEAN     NOT NULL DEFAULT FALSE,
    version         INTEGER     NOT NULL DEFAULT 0,
    actualizado_en  TIMESTAMP,
    actualizado_por VARCHAR(150),
    CONSTRAINT uk_documento_proyecto_paso UNIQUE (proyecto_id, paso_id)
);

CREATE INDEX idx_documentos_proyecto ON documentos (proyecto_id);

-- ---------------------------------------------------------------------------
-- 4. Historial. Append-only: una fila por guardado, nunca se actualiza.
--    Es lo que después sostiene la trazabilidad entre participantes.
-- ---------------------------------------------------------------------------
CREATE TABLE documento_versiones (
    version_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento_id BIGINT      NOT NULL REFERENCES documentos (documento_id) ON DELETE CASCADE,
    contenido    TEXT        NOT NULL,
    autor        VARCHAR(150) NOT NULL,
    autor_tipo   VARCHAR(20)  NOT NULL,
    creado_en    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_versiones_documento ON documento_versiones (documento_id, creado_en DESC);
