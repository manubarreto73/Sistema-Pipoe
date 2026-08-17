CREATE TABLE proyectos (
    proyecto_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios (usuario_id)
);
