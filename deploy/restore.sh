#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

[ -f .env ] && set -a && . ./.env && set +a

FILE="${1:?uso: ./deploy/restore.sh <backup.sql.gz>}"
DB_TEMP="${POSTGRES_DB}_restore_$(date +%s)"

docker compose exec -T db createdb -U "${POSTGRES_USER:-postgres}" "$DB_TEMP"
gunzip -c "$FILE" \
  | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER:-postgres}" -d "$DB_TEMP"

echo "Restaurado en BD temporal: $DB_TEMP"