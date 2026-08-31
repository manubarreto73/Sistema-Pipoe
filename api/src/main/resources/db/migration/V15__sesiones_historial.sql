-- El historial pasa a contarse por sesión de escritura y no por guardado suelto.
--
-- El editor guarda solo cada 2,5 segundos de inactividad, y hasta acá cada guardado insertaba
-- una fila: una tarde de trabajo dejaba cientos de entradas casi idénticas, inservibles como
-- historial. De ahora en más los guardados seguidos de una misma persona sobre un mismo
-- documento actualizan la última fila en vez de agregar otra (ver DocumentoService.guardar).
--
-- Esto deja sin efecto el comentario de V8 que dice que la tabla es append-only. Se anota acá
-- porque aquella migración ya está aplicada y su checksum no se puede tocar.

ALTER TABLE documento_versiones
    ADD COLUMN autor_id       BIGINT,
    ADD COLUMN actualizado_en TIMESTAMP,
    ADD COLUMN guardados      INTEGER NOT NULL DEFAULT 1;

-- Hasta acá sólo se guardaba el nombre del autor, y dos personas homónimas serían
-- indistinguibles. Las filas viejas quedan con autor_id NULL a propósito: una fila sin autor_id
-- nunca se fusiona con un guardado nuevo. Es preferible una entrada de más antes que atribuirle
-- a alguien un texto que puede no ser suyo.

-- creado_en pasa a significar "cuándo empezó la sesión" y actualizado_en "cuándo fue el último
-- guardado". En una fila vieja son el mismo instante, que es justo lo que eran.
UPDATE documento_versiones SET actualizado_en = creado_en;

ALTER TABLE documento_versiones ALTER COLUMN actualizado_en SET NOT NULL;

-- Las consultas del historial ordenan por version_id y no por fecha, porque dos guardados
-- pueden caer en el mismo milisegundo. El índice de V8 es por creado_en y no las cubre.
CREATE INDEX idx_versiones_documento_id ON documento_versiones (documento_id, version_id DESC);
