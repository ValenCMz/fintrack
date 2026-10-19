# Deploy / Cómo correr FinTrack

App dockerizada: Postgres + backend (Spring Boot) + frontend (Next.js), orquestados con
`docker compose`. Dos formas de usarla:

1. **Local (recomendado para uso personal)** — todo en tu Mac, accesible en `localhost`.
2. **Nube (opcional / a futuro)** — en una VM con acceso privado por Tailscale.

---

## 1. Uso local (recomendado)

Requiere **Docker Desktop** instalado y corriendo.

### Levantar
```bash
docker compose up -d --build
```
Abrí **http://localhost:3000** en el navegador.

> Puertos: se configuran en `.env` con `FRONTEND_PORT` (host) y `BACKEND_PORT`. Por
> defecto 3000 y 8080. Si el 3000 ya lo usás en otro proyecto, cambiá `FRONTEND_PORT`
> (y `FRONTEND_URL`/`CORS_ALLOWED_ORIGINS` para que coincidan) y volvé a correr
> `docker compose up -d`.

### Apagar (liberar RAM/CPU)
```bash
docker compose down      # apaga los contenedores; los datos quedan en el volumen
```
Los datos de Postgres viven en el volumen `pgdata`, así que apagar/prender no borra nada.

### Estado y logs
```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
```

### Configuración (`.env`)
Se copia de `.env.example`:
```bash
cp .env.example .env
```
Para uso local los valores por defecto ya sirven:
- `FRONTEND_URL=http://localhost:3000`
- `COOKIE_SECURE=false`
- `JWT_SECRET` → poner uno propio: `openssl rand -hex 64`

El **reset de password por email** solo funciona si completás `MAIL_*` con un SMTP real
(Gmail + *app password*). Sin eso, el resto de la app anda igual.

### Backups
```bash
./deploy/backup.sh                         # genera deploy/backups/fintrack-<fecha>.sql.gz
./deploy/restore.sh deploy/backups/<...>.sql.gz   # ensayo en una BD temporal
```

---

## 2. Despliegue en la nube (opcional / a futuro)

Objetivo: la app viva aunque la Mac esté apagada, con una URL privada (HTTPS) accesible
desde tus dispositivos. Estado: **no desplegado**; runbook de referencia.

### Arquitectura

```
Celu / Mac (navegador)
   │  https://<vm>.<tailnet>.ts.net      (privado, Tailscale/WireGuard)
   ▼
VM (Ubuntu 24.04, ARM)
   ├── tailscaled + Tailscale Serve  →  termina TLS, reenvía a :3000
   ├── frontend (Next.js standalone, :3000)
   │     └── rewrite `/api/*` → backend:8080/api/* (mismo origen, sin CORS)
   ├── backend (Spring Boot, :8080)
   └── db (Postgres 17-alpine, solo red interna)
```

- **Ningún puerto abierto a internet.** Acceso solo por el tailnet.
- **Mismo origen**: el frontend proxea `/api` al backend; sin CORS y la cookie fluye.

### VM (Oracle Cloud Free Tier — opciones realmente gratis)

| Proveedor | Spec | Vigencia | Letra chica |
|---|---|---|---|
| Oracle Always Free (Ampere A1) | **2 OCPU ARM / 12 GB** / 200 GB / 10 TB | Para siempre | Ojo: era 4/24 y lo **bajaron a 2/12 en junio 2026**. Reclama instancias idle (<20% CPU/mem/red por 7 días). Piden tarjeta. |
| Google Cloud e2-micro | 2 vCPU / **1 GB RAM** / 30 GB | Para siempre | Solo 3 regiones de US; 1 GB queda justo para este stack (habría que limitar el JVM). |

Pasos (Oracle):
1. Instancia **Ampere A1 → 2 OCPU / 12 GB**, imagen **Ubuntu 24.04 (arm64)**.
   - Evitá shapes x86 como `E2.1.Micro` (1 GB) o `E5.Flex` (no es free).
   - Cuidado con imágenes viejas (20.04 está EOL y rompe el instalador de Docker).
2. **Red (VCN):** hace falta **Internet Gateway** + ruta `0.0.0.0/0 → IGW` + regla
   ingress TCP 22. Sin eso, la VM no tiene salida ni entrada a internet (síntoma:
   el SSH queda "colgado"). Lo más simple es crear la VCN con el **VCN Wizard →
   "Create VCN with Internet Connectivity"**.
3. SSH: `ssh -i <clave> ubuntu@<ip-pública>`.
4. Instalar Docker con el script oficial (NO usar el paquete `docker-compose-plugin`
   de apt, que falla en distros EOL):
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker ubuntu
   sudo systemctl enable --now docker
   ```
4. Tailscale:
   ```bash
   curl -fsSL https://tailscale.com/install.sh | sh
   sudo systemctl enable --now tailscaled
   sudo tailscale up
   sudo tailscale serve --bg http://localhost:3000
   ```
4. Clonar y desplegar:
   ```bash
   git clone https://github.com/ValenCMz/fintrack.git && cd fintrack
   cp .env.example .env      # FRONTEND_URL=https://<vm>.<tailnet>.ts.net, COOKIE_SECURE=true, MAIL_*, JWT_SECRET
   docker compose up -d --build
   ```
5. Cerrar el puerto 22 público una vez que entrás por Tailscale.

### Migrar de host
Con todo dockerizado, migrar es: `./deploy/backup.sh` en el origen → levantar el mismo
compose en el destino → `pg_restore`. La versión de Postgres debe ser igual o posterior.

---

## Riesgos y decisiones

- Migraciones con Flyway (`ddl-auto=validate`): el esquema se versiona en `db/migration`; las bases existentes se baselinan y solo reciben las migraciones nuevas.
- `/auth/register` queda abierto; en local/no público no es problema.
- En local, el servicio solo existe mientras Docker corre; los datos persisten en el volumen.
