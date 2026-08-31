#!/usr/bin/env bash
# Emite el primer certificado de Let's Encrypt.
#
# Hay un circulo que romper: nginx no arranca sin certificado, y Let's Encrypt no valida el
# dominio sin nginx sirviendo el puerto 80. Se resuelve con un certificado falso que dura un
# dia, suficiente para que nginx levante y pueda responder el desafio.
set -euo pipefail

cd "$(dirname "$0")"
set -a; source .env; set +a

if [ -z "${DOMINIO:-}" ] || [ -z "${CERTBOT_EMAIL:-}" ]; then
    echo "Faltan DOMINIO o CERTBOT_EMAIL en el .env"; exit 1
fi

RUTA="/etc/letsencrypt/live/${DOMINIO}"
COMPOSE="docker compose -f docker-compose.prod.yml --env-file .env"

echo "==> Certificado temporal para que nginx pueda arrancar"
$COMPOSE run --rm --entrypoint "sh -c '
  mkdir -p ${RUTA} &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout ${RUTA}/privkey.pem \
    -out ${RUTA}/fullchain.pem \
    -subj /CN=localhost'" certbot

# --force-recreate y no un simple `up -d`: si nginx quedo en bucle de reinicio del primer
# arranque, `up -d` lo deja como esta y nunca llega a tomar el certificado temporal.
echo "==> Levantando nginx"
$COMPOSE up -d --force-recreate web

echo "==> Esperando a que nginx responda"
LISTO=no
for _ in $(seq 1 30); do
    CODIGO=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost/ || true)
    if [ "${CODIGO}" != "000" ]; then
        echo "    nginx responde (HTTP ${CODIGO})"; LISTO=si; break
    fi
    sleep 2
done
if [ "${LISTO}" != "si" ]; then
    echo "ERROR: nginx no llego a arrancar. Mira:  ${COMPOSE} logs web | tail -30"
    exit 1
fi

# Esta verificacion evita quemar intentos: Let's Encrypt permite 5 validaciones fallidas por
# hora y por dominio. Si el desafio no se lee desde internet, el certificado ni se pide.
echo "==> Verificando que el desafio se pueda leer desde internet"
PRUEBA="prueba-$(date +%s)"
$COMPOSE run --rm --entrypoint "sh -c '
  mkdir -p /var/www/certbot/.well-known/acme-challenge &&
  echo ${PRUEBA} > /var/www/certbot/.well-known/acme-challenge/${PRUEBA}'" certbot

# Se prueban los dos nombres porque el certificado los incluye a los dos: si www no valida,
# el intento se pierde igual, aunque el dominio pelado este perfecto.
FALLO=""
for NOMBRE in "${DOMINIO}" "www.${DOMINIO}"; do
    RESPUESTA=$(curl -s --max-time 10 "http://${NOMBRE}/.well-known/acme-challenge/${PRUEBA}" || true)
    if [ "${RESPUESTA}" != "${PRUEBA}" ]; then
        echo "    ${NOMBRE}: FALLA (devolvio '${RESPUESTA:-nada}')"
        FALLO="si"
    else
        echo "    ${NOMBRE}: ok"
    fi
done

$COMPOSE run --rm --entrypoint "rm -f /var/www/certbot/.well-known/acme-challenge/${PRUEBA}" certbot

if [ -n "${FALLO}" ]; then
    echo "ERROR: el desafio no se lee desde internet en alguno de los dos nombres."
    echo "  1. DNS:        dig +short ${DOMINIO} ; dig +short www.${DOMINIO}"
    echo "  2. Puerto 80 desde afuera (firewall del panel del proveedor)"
    echo "  3. ufw:        sudo ufw status"
    echo "  4. Cloudflare: si el DNS esta ahi, tiene que estar en 'DNS only' (nube gris)"
    echo "No se pidio el certificado, no gastaste ningun intento."
    exit 1
fi

# Los dos nombres en UN solo certificado: nginx sirve www desde la misma ruta de
# /etc/letsencrypt/live/${DOMINIO}, y dos certificados separados obligarian a dos rutas.
echo "==> Borrando el temporal y pidiendo el real"
$COMPOSE run --rm --entrypoint "rm -rf /etc/letsencrypt/live/${DOMINIO} /etc/letsencrypt/archive/${DOMINIO} /etc/letsencrypt/renewal/${DOMINIO}.conf" certbot

$COMPOSE run --rm --entrypoint "certbot certonly --webroot -w /var/www/certbot \
  --email ${CERTBOT_EMAIL} -d ${DOMINIO} -d www.${DOMINIO} --rsa-key-size 2048 \
  --agree-tos --no-eff-email --non-interactive" certbot

echo "==> Recargando nginx con el certificado real"
$COMPOSE exec web nginx -s reload

echo
echo "LISTO. Probalo desde afuera:  https://${DOMINIO}"
