CREATE TABLE parametros (
    parametro_id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    max_proyectos_por_usuario       INTEGER NOT NULL,
    max_colaboradores_por_proyecto  INTEGER NOT NULL
);

INSERT INTO parametros (max_proyectos_por_usuario, max_colaboradores_por_proyecto)
VALUES (5, 10);
