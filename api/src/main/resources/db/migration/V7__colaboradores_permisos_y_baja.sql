-- Baja lógica del colaborador: se deja de listar y no puede volver a entrar, pero la fila
-- sobrevive porque más adelante va a estar referenciada por el contenido del proyecto.
ALTER TABLE colaboradores ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

-- Permisos por fase. El pipoe tiene 5 fases fijas y cada colaborador tiene un nivel en
-- cada una, así que la tabla siempre guarda 5 filas por colaborador.
CREATE TABLE colaborador_permisos (
    permiso_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    colaborador_id BIGINT      NOT NULL REFERENCES colaboradores (colaborador_id) ON DELETE CASCADE,
    fase           INTEGER     NOT NULL CHECK (fase BETWEEN 1 AND 5),
    nivel          VARCHAR(20) NOT NULL,
    CONSTRAINT uk_permiso_colaborador_fase UNIQUE (colaborador_id, fase)
);

-- Los colaboradores que ya existían arrancan con sólo lectura en las 5 fases: es el nivel
-- más restrictivo, y el dueño del proyecto puede subirlo desde la pantalla de colaboradores.
INSERT INTO colaborador_permisos (colaborador_id, fase, nivel)
SELECT c.colaborador_id, f.fase, 'LECTURA'
FROM colaboradores c
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS f (fase);
