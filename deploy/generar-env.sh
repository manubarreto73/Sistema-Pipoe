#!/usr/bin/env bash
# Crea el .env del servidor con los secretos generados aca mismo.
#
# Generarlos en el servidor y no copiarlos desde otra maquina evita el problema clasico: pegar
# un valor largo en una terminal, que se trunque sin que se note, y pasar horas buscando por
# que la firma de los tokens no valida.
set -euo pipefail
cd "$(dirname "$0")"

if [ -e .env ]; then
    echo "Ya existe un .env en $(pwd)."
    echo "Si queres rehacerlo:  mv .env .env.viejo && bash generar-env.sh"
    exit 1
fi

fijar() {
    CLAVE="$1" VALOR="$2" python3 -c '
import os, io, re
clave = os.environ["CLAVE"]
valor = os.environ["VALOR"]
texto = io.open(".env", encoding="utf-8").read()
patron = re.compile(r"(?m)^" + re.escape(clave) + r"=.*$")
if not patron.search(texto):
    raise SystemExit("No existe la clave " + clave + " en .env")
texto = patron.sub(lambda _: clave + "=" + valor, texto, count=1)
io.open(".env", "w", encoding="utf-8", newline="\n").write(texto)
'
}

enmascarar() {
    printf '%.4s… (%d caracteres)\n' "$1" "${#1}"
}

cp .env.prod.example .env
chmod 600 .env

echo "==> Generando secretos aleatorios"
# El alfabeto base64 no incluye $, asi que ningun valor se interpola al leer el .env.
DB_PASS="$(openssl rand -base64 24)"
REDIS_PASS="$(openssl rand -base64 24)"
# 48 bytes son 384 bits. HS256 pide 256 como minimo.
JWT="$(openssl rand -base64 48 | tr -d '\n')"

fijar DB_PASSWORD    "${DB_PASS}"
fijar REDIS_PASSWORD "${REDIS_PASS}"
fijar JWT_SECRET_KEY "${JWT}"

echo
read -rp "Dominio (sin https:// y sin barra final): " DOM
read -rp "Email para los avisos de Let's Encrypt:  " CERT_EMAIL

if [ -z "${DOM}" ] || [ -z "${CERT_EMAIL}" ]; then
    rm -f .env; echo "Faltan datos. No se creo el .env."; exit 1
fi

fijar DOMINIO              "${DOM}"
fijar CERTBOT_EMAIL        "${CERT_EMAIL}"
fijar CORS_ALLOWED_ORIGINS "https://${DOM}"

echo
echo "--- Primer administrador (se crea solo si la base no tiene ninguno) ---"
read -rp  "Email del administrador: " EMAIL_ADMIN
read -rsp "Contrasena, minimo 8 caracteres (no se muestra): " PASS_ADMIN; echo
read -rsp "Repetir contrasena: " PASS_REPE; echo

if [ "${PASS_ADMIN}" != "${PASS_REPE}" ]; then
    rm -f .env; echo "Las contrasenas no coinciden. No se creo el .env."; exit 1
fi
# El mismo minimo que exige AdminBootstrap y que rige en toda la aplicacion. Si alguno de los
# dos cambia, tienen que cambiar los dos: cuando no coincidieron, este script acepto una clave
# que el arranque despues rechazo, y la cuenta no se creo sin que nada lo dijera.
if [ ${#PASS_ADMIN} -lt 8 ]; then
    rm -f .env; echo "La contrasena tiene que tener al menos 8 caracteres."; exit 1
fi

fijar ADMIN_INICIAL_EMAIL    "${EMAIL_ADMIN}"
fijar ADMIN_INICIAL_PASSWORD "${PASS_ADMIN}"

echo
echo "--- Correo saliente ---"
echo "El sistema DEPENDE de esto: las contrasenas de usuarios y colaboradores viajan solo por"
echo "correo. Si no sale, nadie puede entrar. Se puede saltear y completarlo despues."
read -rp "Host SMTP [smtp-relay.brevo.com, enter para saltear]: " SMTP_HOST
if [ -n "${SMTP_HOST}" ]; then
    read -rp  "Puerto [587]: " SMTP_PORT
    # El usuario de Brevo NO es el remitente: es una credencial generada, tipo
    # 9a1b2c001@smtp-brevo.com. Confundirlos es el error mas comun de esta pantalla.
    read -rp  "Usuario SMTP (el que da el panel, no el remitente): " SMTP_USER
    read -rsp "Contrasena: " SMTP_PASS; echo
    read -rp  "Remitente [no-reply@modelopipoe.com]: " SMTP_FROM
    SMTP_FROM="${SMTP_FROM:-no-reply@modelopipoe.com}"

    fijar MAIL_HOST     "${SMTP_HOST}"
    fijar MAIL_PORT     "${SMTP_PORT:-587}"
    fijar MAIL_USERNAME "${SMTP_USER}"
    fijar MAIL_PASSWORD "${SMTP_PASS}"
    fijar MAIL_FROM     "${SMTP_FROM}"
fi

echo
echo "==> .env creado con permisos 600. Resumen:"
echo "    DOMINIO         ${DOM}"
echo "    DB_PASSWORD     $(enmascarar "${DB_PASS}")"
echo "    REDIS_PASSWORD  $(enmascarar "${REDIS_PASS}")"
echo "    JWT_SECRET_KEY  $(enmascarar "${JWT}")"
echo "    ADMIN_INICIAL   ${EMAIL_ADMIN}"
echo "    MAIL_HOST       ${SMTP_HOST:-(sin configurar)}"
echo
echo "OJO: DB_PASSWORD queda grabada en el volumen de Postgres en el primer arranque."
echo "Cambiarla despues en el .env NO cambia la de la base y la API deja de conectarse."
