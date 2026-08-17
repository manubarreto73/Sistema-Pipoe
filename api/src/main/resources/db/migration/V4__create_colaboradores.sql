CREATE TABLE colaboradores (
    colaborador_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    proyecto_id    BIGINT NOT NULL REFERENCES proyectos (proyecto_id),
    CONSTRAINT uk_colaborador_proyecto_email UNIQUE (proyecto_id, email)
);
