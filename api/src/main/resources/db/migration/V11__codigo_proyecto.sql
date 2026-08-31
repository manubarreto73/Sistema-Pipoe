-- Código de acceso del proyecto: es lo que un colaborador escribe para entrar.
--
-- Hasta acá el login del colaborador pedía el NOMBRE del proyecto, y eso tenía dos agujeros:
--
--   1. El nombre es único, pero con distinción de mayúsculas. "Proyecto Aldea" y
--      "proyecto aldea" son dos filas legítimas para Postgres y el mismo nombre para
--      cualquier persona: un colaborador con el mismo email y la misma clave en ambos
--      entraba a uno de los dos sin ninguna forma de saber a cuál.
--   2. El dueño puede renombrar su proyecto cuando quiera, y ese día todos sus
--      colaboradores dejaban de poder entrar con un "credenciales inválidas" que no
--      explicaba nada.
--
-- El código no se puede cambiar y no se deriva del nombre, así que no tiene ninguno de los
-- dos problemas.
--
-- Alfabeto sin 0/O, 1/I/L: el código se dicta y se copia a mano, y esos pares se confunden.
-- Cuatro caracteres sobre 31 símbolos son ~923.000 combinaciones, de sobra para este sistema;
-- la unicidad igual la garantiza la constraint, no el tamaño del espacio.

ALTER TABLE proyectos ADD COLUMN codigo VARCHAR(12);

DO $$
DECLARE
    alfabeto CONSTANT TEXT := '23456789ABCDEFGHJKMNPQRSTUVWXYZ';
    fila     RECORD;
    intento  TEXT;
    i        INTEGER;
BEGIN
    FOR fila IN SELECT proyecto_id FROM proyectos LOOP
        LOOP
            intento := 'PIPOE-';

            FOR i IN 1..4 LOOP
                intento := intento
                    || substr(alfabeto, 1 + floor(random() * length(alfabeto))::INTEGER, 1);
            END LOOP;

            EXIT WHEN NOT EXISTS (SELECT 1 FROM proyectos WHERE codigo = intento);
        END LOOP;

        UPDATE proyectos SET codigo = intento WHERE proyecto_id = fila.proyecto_id;
    END LOOP;
END $$;

ALTER TABLE proyectos ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE proyectos ADD CONSTRAINT uk_proyecto_codigo UNIQUE (codigo);
