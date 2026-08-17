CREATE TABLE usuarios (
    usuario_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(150) NOT NULL UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE
);
