-- El colaborador pasa a tener contraseña propia (hash BCrypt), generada al darlo de alta
-- y enviada por mail. Antes el login sólo pedía nombre de proyecto + email.
--
-- Las filas que ya existan quedan con hash vacío: BCrypt nunca hace match contra '',
-- y ColaboradorService.login rechaza explícitamente el hash en blanco. Esos colaboradores
-- tienen que volver a ser dados de alta para recibir su clave.
ALTER TABLE colaboradores ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE colaboradores ALTER COLUMN password DROP DEFAULT;
