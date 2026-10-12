#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

[ -f .env ] && set -a && . ./.env && set +a

mkdir -p deploy/backups
STAMP=$(date +%Y-%m-%d_%H-%M-%S)
FILE="deploy/backups/fintrack-${STAMP}.sql.gz"

docker compose exec -T db pg_dump -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB}" \
  | gzip > "$FILE"

ls -t deploy/backups/fintrack-*.sql.gz | tail -n +8 | xargs -r rm --
echo "Backup listo: $FILE"