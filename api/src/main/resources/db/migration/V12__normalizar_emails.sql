-- El email se guarda normalizado: sin espacios en los extremos y en minúsculas.
--
-- Postgres compara VARCHAR distinguiendo mayúsculas, así que hasta acá "Ana@correo.com" y
-- "ana@correo.com" eran dos identidades distintas para el sistema y el mismo buzón para la
-- persona. Las consecuencias no eran cosméticas:
--
--   - La validación de solicitud duplicada (existsByEmailAndEstado) no las veía iguales, así
--     que una segunda solicitud con el mismo correo escrito distinto entraba como nueva.
--   - Al aprobar las dos, usuarios.email UNIQUE tampoco las veía iguales: se creaban DOS
--     cuentas para la misma persona, cada una con su clave enviada por mail. Al entrar con
--     una, la clave de la otra no servía, y no había ningún error que lo explicara.
--
-- La normalización se aplica al escribir (ver Emails.normalizar) y el CHECK de acá abajo la
-- hace obligatoria: si alguna vez se agrega un alta que se la saltee, falla al insertar en
-- lugar de crear en silencio una identidad duplicada.

-- ---------------------------------------------------------------------------
-- 1. Antes de tocar nada: si ya existen dos filas que sólo se distinguen por
--    mayúsculas, pasarlas a minúsculas violaría la constraint de unicidad a
--    mitad de la migración. Se aborta con un mensaje que dice cuáles son, para
--    unificarlas a mano; adivinar cuál conservar no es decisión de un script.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    colisiones TEXT;
BEGIN
    SELECT string_agg(email, ', ') INTO colisiones
    FROM (
        SELECT lower(btrim(email)) AS email
        FROM usuarios
        GROUP BY lower(btrim(email))
        HAVING count(*) > 1
    ) duplicados;

    IF colisiones IS NOT NULL THEN
        RAISE EXCEPTION
            'Hay usuarios que sólo se distinguen por mayúsculas: %. Unificalos antes de migrar.',
            colisiones;
    END IF;

    SELECT string_agg(email, ', ') INTO colisiones
    FROM (
        SELECT lower(btrim(email)) AS email
        FROM colaboradores
        GROUP BY proyecto_id, lower(btrim(email))
        HAVING count(*) > 1
    ) duplicados;

    IF colisiones IS NOT NULL THEN
        RAISE EXCEPTION
            'Hay colaboradores del mismo proyecto que sólo se distinguen por mayúsculas: %.',
            colisiones;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Los datos que ya están.
-- ---------------------------------------------------------------------------
UPDATE usuarios           SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
UPDATE colaboradores      SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));
UPDATE solicitudes_acceso SET email = lower(btrim(email)) WHERE email <> lower(btrim(email));

-- ---------------------------------------------------------------------------
-- 3. La garantía, para que no vuelva a pasar.
-- ---------------------------------------------------------------------------
ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_email_normalizado CHECK (email = lower(btrim(email)));
ALTER TABLE colaboradores
    ADD CONSTRAINT ck_colaboradores_email_normalizado CHECK (email = lower(btrim(email)));
ALTER TABLE solicitudes_acceso
    ADD CONSTRAINT ck_solicitudes_email_normalizado CHECK (email = lower(btrim(email)));
