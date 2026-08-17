CREATE TABLE solicitudes_acceso (
    solicitud_acceso_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_completo     VARCHAR(150) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    pais                VARCHAR(100) NOT NULL,
    universidad         VARCHAR(150) NOT NULL,
    uso                 VARCHAR(500) NOT NULL,
    estado              VARCHAR(20)  NOT NULL,
    fecha_solicitud     TIMESTAMP    NOT NULL,
    fecha_resolucion    TIMESTAMP
);

-- El email no es único: tras un rechazo se puede volver a pedir acceso.
-- Que no haya dos PENDIENTES del mismo email lo valida SolicitudAccesoService.
CREATE INDEX idx_solicitudes_acceso_estado ON solicitudes_acceso (estado, fecha_solicitud DESC);
