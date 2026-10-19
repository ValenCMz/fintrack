# fintrack
Personal finance backend built with Java and PostgreSQL, featuring a normalized relational model for financial transactions, accounts, debts, savings goals, and recurring expenses. Designed as a foundation for a full-stack application with future AI integration via MCP.

## Cómo correr (local)

Requiere Docker Desktop. Todo el stack (Postgres + backend + frontend) se levanta con:

```bash
cp .env.example .env      # ajustar JWT_SECRET
docker compose up -d --build
```

App en **http://localhost:3000**. Para apagar: `docker compose down` (los datos persisten).

Detalle completo (config, backups, y despliegue opcional en la nube): `docs/deploy.md`.
