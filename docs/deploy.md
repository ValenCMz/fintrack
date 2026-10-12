# Deploy — FinTrack en Oracle Cloud Free Tier (dockerizado)

> Estado: **Fase 0 (código) completa y verificada** el 2026-10-09:
> `mvn test` 127/127 verde, `npm run build` verde, stack completo (`docker compose up`)
> probado de punta a punta en local (register, login, refresh por cookie, health UP),
> backup/restore probados (`deploy/backups/`). Pendientes: Fases 1-4 (VM + Tailscale).
>
> Decisión de contexto: usar la app en solitario, gratis. Estado portátil: la base
> vive en el volumen de Postgres y los secrets en `.env`; el resto se reconstruye
> idéntico con `docker compose`. Migrar = backup + levantar el compose en otro lado.

## Arquitectura

```
Celu / Mac (navegador)
   │  Tailscale app encendida (WireGuard, cifrado punta a punta)
   ▼
VM Oracle (Ampere A1, free tier)
   │  Tailscale Serve  →  https://<vm>.<tailnet>.ts.net   (TLS automático)
   ▼
frontend  (Next 16, standalone, :3000)
   │  rewrites de Next: /api/*  →  backend (mismo origen, sin CORS)
   ▼
backend  (Spring Boot 4 / Java 21, :8080, interno)
   │  docker network interna (db no expuesta)
   ▼
Postgres 17 (volumen `pgdata`, solo desde el network interno)
```

Decisiones tomadas:

- **Sin puertos públicos.** La VM no abre 80/443; la app solo es alcanzable por
  dispositivos del tailnet (privada para datos financieros). SSH tampoco público.
- **Mismo origen** (frontend proxea `/api`): no hay CORS en producción y la cookie
  `refreshTokenFinTrack` fluye normal.
- **COOKIE_SECURE=true** en la VM (la página es HTTPS por Tailscale).
- **SMTP obligatorio** (Gmail + app password): sin él, el flujo de reset de password
  no llega y quedarías sin acceso.
- `ddl-auto=update` se mantiene: válido para uso personal, no es estrategia de
  migraciones con formato. Conocido.

## Variables de entorno (`.env`)

Ver `.env.example`. Claves que importan:

| Variable | Nota |
|---|---|
| `JWT_SECRET` | generar con `openssl rand -hex 64` |
| `MAIL_*` | SMTP de Gmail con **app password** |
| `FRONTEND_URL` | la URL final del tailnet (`https://vm.tunombre.ts.net`) |
| `COOKIE_SECURE` | `true` en la VM |
| `CORS_ALLOWED_ORIGINS` | en prod no hace falta (mismo origen); en dev local queda `http://localhost:3000` |

## Comandos

```bash
# Build + levantar (dev local, raíz del repo)
docker compose up -d --build

# Productivo en la VM
docker compose up -d --build   # con .env apuntando a la config de prod

# Logs
docker compose logs -f backend frontend

# Backup manual (crea deploy/backups/fintrack-fecha.sql.gz)
./deploy/backup.sh

# Restore a una BD temporal (ensayo)
./deploy/restore.sh deploy/backups/<archivo>.sql.gz
```

## Fases

**Fase 0 — Código (repo, en curso):**
- CORS por env (`SecurityConfig`), `FRONTEND_URL` por env, actuator para healthcheck.
- `next.config.ts`: `output: standalone` + rewrite de `/api` al backend (leyendo
  `BACKEND_INTERNAL_URL`, default localhost para dev).
- `Dockerfile` backend (multi-stage maven→jre, usuario no-root) y frontend (standalone).
- `docker-compose.yml` (db/backend/frontend, healthchecks, `restart: unless-stopped`).
- `deploy/backup.sh` / `deploy/restore.sh` (pg_dump + retention 7 días).
- Verificación local: `mvn -o test` verde + `npm run build` verde + `docker compose up` verde.

**Fase 1 — VM Oracle (manual, coordinado):**
- Alta free tier: instancia Ampere A1 (Ubuntu 24.04), clave SSH de Oracle.
- VCN/inbound cerrado (solo SSH por tailnet). `unattended-upgrades`.
- Docker Engine + compose plugin.

**Fase 2 — Tailscale (manual, coordinado):**
- Tailscale en VM, Mac y celular (mismo tailnet).
- `tailscale serve --bg http://localhost:3000` → `https://<vm>.<tailnet>.ts.net`.

**Fase 3 — Deploy (manual, coordinado):**
- Clonar repo en la VM, armar `.env`, `docker compose up -d --build`.
- Healthcheck: `GET https://.../api/actuator/health` → `status: UP`.
- Probar: login (cookie Secure), alta de gasto, reset de password con el link del tailnet.

**Fase 4 — Operación:**
- cron diario `deploy/backup.sh` en la VM (retention 7).
- Ensayo de restore cada tanto en una BD temporal.
- Migrar a otro hosting = backup + mismo compose + restore (todo documentado arriba).

## Riesgos asumidos

- Free tier de Oracle limita CPU sostenida alta; no lo vas a notar acá.
- Registro abierto (`/auth/register`) pero la app es privada del tailnet.
- `ddl-auto=update` (ver arriba). Backups diarios son la red de seguridad.