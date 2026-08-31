-- Comentarios sobre el documento de un paso.
--
-- Cuelgan de documentos y no de pasos: el paso es catálogo compartido por todos los
-- proyectos, el documento es el del proyecto. Como el "producto" de cada fase (Plan de
-- promoción, Diagnóstico situacional...) ya es un paso más con su propio documento, esta
-- única tabla cubre tanto los pasos del despliegue como los productos, sin un segundo
-- camino para el mismo caso.
--
-- NO se guarda a qué parte del texto se refiere el comentario. Es deliberado: anclar un
-- comentario a un fragmento obliga a reubicarlo en cada edición del documento, y acá el
-- comentario es sobre el documento entero.
--
-- El autor se guarda por partida doble, igual que en documento_versiones:
--   - autor (el nombre) queda congelado, para que el historial siga leyéndose aunque después
--     la persona se cambie el nombre o la den de baja del proyecto;
--   - autor_tipo + autor_id identifican la sesión, que es lo que permite responder "¿este
--     comentario es mío?" a la hora de borrarlo. Un colaborador y un usuario pueden compartir
--     el id, así que el tipo hace falta: sin él, el colaborador 3 podría borrar comentarios
--     del usuario 3.
--
-- Sin FK a usuarios ni a colaboradores, por lo mismo que el nombre va copiado: el comentario
-- sobrevive al borrado del proyecto de quien lo escribió.

CREATE TABLE comentarios (
    comentario_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento_id  BIGINT       NOT NULL REFERENCES documentos (documento_id) ON DELETE CASCADE,
    texto         TEXT         NOT NULL,
    autor         VARCHAR(150) NOT NULL,
    autor_tipo    VARCHAR(20)  NOT NULL,
    autor_id      BIGINT       NOT NULL,
    creado_en     TIMESTAMP    NOT NULL
);

-- El listado siempre es "los de este documento, del más nuevo al más viejo".
CREATE INDEX idx_comentarios_documento ON comentarios (documento_id, creado_en DESC);
