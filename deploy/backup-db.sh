#!/usr/bin/env bash
# Copia comprimida de la base. Va en cron todas las noches.
#
# El backup del proveedor suele ser semanal: hasta 7 dias de perdida posible. Este achica esa
# ventana a un dia.
set -euo pipefail

cd "$(dirname "$0")"
set -a; source .env; set +a

DESTINO="${BACKUP_DIR:-/var/backups/pipoe}"
RETENCION_DIAS="${BACKUP_RETENTION_DAYS:-30}"
BASE="${DB_NAME:-pipoe}"
ARCHIVO="${DESTINO}/${BASE}_$(date +%Y-%m-%d_%H%M).sql.gz"

mkdir -p "${DESTINO}"

docker compose -f docker-compose.prod.yml --env-file .env exec -T db \
    pg_dump -U "${DB_USERNAME}" -d "${BASE}" | gzip > "${ARCHIVO}"

# Un backup que falla en silencio es peor que no tenerlo: si quedo vacio se borra y el script
# termina en error, para que cron lo reporte en vez de dejar un archivo inservible.
if [ ! -s "${ARCHIVO}" ]; then
    echo "$(date -Is) ERROR: el backup quedo vacio, se elimina" >&2
    rm -f "${ARCHIVO}"
    exit 1
fi

find "${DESTINO}" -name "${BASE}_*.sql.gz" -mtime "+${RETENCION_DIAS}" -delete

echo "$(date -Is) OK $(du -h "${ARCHIVO}" | cut -f1) ${ARCHIVO}"
