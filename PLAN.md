# Fintrack — Plan de Implementación

## Arquitectura General

```
PostgreSQL ← Java Spring Boot (REST API) ←┬─ Next.js Frontend (Web App)
                                           └─ Node.js MCP Server ←┬─ Telegram Bot
                                                                   └─ Ollama (IA Local)
```

---

## Fase 1: Completar Backend REST API (Java / Spring Boot)

### 1.1 Repositorios JPA
Crear interfaces `@Repository` para las 9 entidades:
- `UserRepository`
- `AccountRepository`
- `CategoryRepository`
- `TransactionRepository`
- `FixedExpendeRepository`
- `DebtRepository`
- `CardRepository`
- `SavingGoalRepository`
- `MonotributoRepository`

### 1.2 DTOs
Crear Data Transfer Objects para request/response de cada entidad:
- `UserDto`, `AccountDto`, `CategoryDto`, `TransactionDto`, `FixedExpendeDto`, `DebtDto`, `CardDto`, `SavingGoalDto`, `MonotributoDto`
- DTOs de autenticación: `LoginRequest`, `LoginResponse`, `RegisterRequest`

### 1.3 Servicios
Implementar lógica de negocio:
- `UserService` — registro, login, gestión de perfil
- `TransactionService` — CRUD + filtros por fecha/categoría/cuenta/tipo
- `AccountService` — CRUD + balance por cuenta
- `CategoryService` — CRUD
- `FixedExpendeService` — CRUD + recordatorio de vencimientos
- `DebtService` — CRUD + seguimiento de pagos
- `CardService` — CRUD + cálculo de próximos vencimientos
- `SavingGoalService` — CRUD + progreso hacia la meta
- `MonotributoService` — CRUD + estado de pagos

### 1.4 Controladores REST
Exponer endpoints RESTful:

| Recurso | Endpoints |
|---------|-----------|
| Auth | `POST /api/auth/login`, `POST /api/auth/register` |
| Transactions | `GET/POST /api/transactions`, `GET/PUT/DELETE /api/transactions/{id}`, `GET /api/transactions/summary` |
| Accounts | `GET/POST /api/accounts`, `GET/PUT/DELETE /api/accounts/{id}` |
| Categories | `GET/POST /api/categories`, `GET/PUT/DELETE /api/categories/{id}` |
| Fixed Expenses | `GET/POST /api/fixed-expenses`, `GET/PUT/DELETE /api/fixed-expenses/{id}` |
| Debts | `GET/POST /api/debts`, `GET/PUT/DELETE /api/debts/{id}` |
| Cards | `GET/POST /api/cards`, `GET/PUT/DELETE /api/cards/{id}` |
| Saving Goals | `GET/POST /api/saving-goals`, `GET/PUT/DELETE /api/saving-goals/{id}` |
| Monotributo | `GET/POST /api/monotributo`, `GET/PUT/DELETE /api/monotributo/{id}` |

### 1.5 Autenticación
- Migrar de Basic Auth a JWT (JSON Web Tokens)
- Endpoint de login devuelve access token + refresh token
- Filtro de seguridad que valida JWT en cada request
- Eliminar usuario hardcodeado `admin/admin`

### 1.6 Reportes y Analíticas
- `GET /api/reports/monthly-summary` — ingresos vs egresos por mes
- `GET /api/reports/by-category` — gastos agrupados por categoría
- `GET /api/reports/balance` — balance general por cuenta
- `GET /api/reports/projections` — proyección de ahorro/gastos futuros

### 1.7 Validaciones y Manejo de Errores
- Validación de campos con Bean Validation (`@NotBlank`, `@Positive`, etc.)
- Manejo global de excepciones con `@ControllerAdvice`
- Respuestas de error estandarizadas

---

## Fase 2: Conectar Frontend al Backend (Next.js / React)

### 2.1 API Client
- Crear `lib/api.ts` — cliente HTTP tipado con fetch/axios
- Interceptor para adjuntar JWT en cada request
- Tipos TypeScript para todas las entidades y DTOs

### 2.2 Autenticación en Frontend
- Página de Login (`/login`)
- Contexto de autenticación (`AuthProvider`)
- Almacenar JWT en httpOnly cookie o localStorage
- Redirección automática si no hay sesión
- Botón de logout en sidebar

### 2.3 Reemplazar Datos Mock por API Real
- **Dashboard** (`/`): stats reales, transacciones recientes, metas de ahorro reales
- **Ingresos** (`/ingresos`): listado real con filtros y paginación
- **Egresos** (`/egresos`): listado real con filtros y paginación

### 2.4 Completar Páginas Placeholder
- **Tarjetas** (`/tarjetas`): listado, crear, editar, eliminar
- **Deudas** (`/deudas`): listado, crear, editar, eliminar, marcar como pagada
- **Ahorros** (`/ahorros`): metas de ahorro con progreso, crear/editar/eliminar
- **Monotributo** (`/monotributo`): gestión de pagos mensuales
- **Gastos Fijos** (`/gastos-fijos`): listado, crear, editar, eliminar
- **Configuración** (`/settings`): perfil de usuario, preferencias

### 2.5 Formularios
- Componentes de formulario reutilizables (react-hook-form + zod)
- Selects dinámicos para cuentas y categorías
- Date pickers para fechas
- Validación client-side y server-side

### 2.6 Manejo de Estado Global
- Zustand o React Context para estado global (usuario, cuentas, categorías)
- React Query (TanStack Query) para caché y sincronización de datos del servidor

---

## Fase 3: MCP Server + Telegram Bot (Node.js / TypeScript)

### 3.1 Proyecto MCP Server
- Nuevo proyecto en `fintrack-mcp/`
- Usar `@modelcontextprotocol/sdk` para implementar el servidor MCP
- Comunicación con el backend Java vía REST API (fetch/axios)

### 3.2 Herramientas MCP (Tools)

| Tool | Descripción |
|------|-------------|
| `add_transaction` | Registrar ingreso o egreso |
| `list_transactions` | Listar transacciones con filtros |
| `get_balance` | Obtener balance por cuenta |
| `get_monthly_summary` | Resumen mensual (ingresos vs egresos) |
| `get_category_report` | Gastos agrupados por categoría |
| `add_saving_goal` | Crear meta de ahorro |
| `list_saving_goals` | Ver metas de ahorro con progreso |
| `list_debts` | Ver deudas activas |
| `list_upcoming_payments` | Próximos vencimientos (tarjetas, monotributo, gastos fijos) |
| `get_accounts` | Listar cuentas disponibles |
| `get_categories` | Listar categorías disponibles |

### 3.3 Telegram Bot
- Crear bot con @BotFather y obtener token
- Usar **grammY** como framework del bot
- Comandos del bot:
  - `/start` — bienvenida y explicación
  - `/ingreso <monto> <descripción>` — registrar ingreso
  - `/egreso <monto> <descripción>` — registrar egreso
  - `/balance` — ver balance general
  - `/resumen` — resumen del mes
  - `/gastos` — gastos por categoría
  - `/vencimientos` — próximos pagos
  - `/metas` — metas de ahorro
  - `/help` — ayuda y lista de comandos
- Procesamiento de lenguaje natural simple para mensajes sin comando (ej: "Gasté 5000 en supermercado")

### 3.4 Autenticación del Bot
- Vincular cuenta de Telegram con usuario de la app (web o vía bot)
- Almacenar mapping `chatId → userId`

---

## Fase 4: Chatbot con IA (Ollama)

### 4.1 Integración con Ollama
- Instalar y configurar Ollama localmente
- Elegir modelo (ej: `llama3.2`, `mistral`, `qwen2.5`)
- Exponer endpoint en el MCP server para consultas al LLM

### 4.2 Chat en Frontend
- Conectar la página `/chat` al endpoint de IA del MCP server
- El chat envía el historial de transacciones y metas como contexto
- Soporte para streaming de respuestas (Server-Sent Events)

### 4.3 Funcionalidades del Chat IA
- Consejos financieros personalizados basados en gastos del usuario
- Análisis de patrones de gasto
- Sugerencias de ahorro
- Explicación de conceptos financieros/inversiones
- Procesar lenguaje natural para crear transacciones ("Registrá un gasto de 2000 en comida")
- Generar reportes mensuales en lenguaje natural

### 4.4 Mejoras Futuras (Opcional)
- RAG con PDFs de extractos bancarios
- Integración con APIs de cotización (dólar, crypto)
- Recomendaciones de inversión simples

---

## Fase 5: Infraestructura y DevOps (Futuro)

- Dockerizar todos los servicios (docker-compose.yml)
- Variables de entorno (.env) para todas las credenciales
- Tests unitarios y de integración
- CI/CD con GitHub Actions
- Health checks y monitoreo

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend API | Java 21, Spring Boot 4.0.1, Spring Data JPA, Spring Security |
| Base de Datos | PostgreSQL 16 |
| Frontend Web | Next.js 16, React 19, TypeScript 5, Tailwind CSS v4, shadcn/ui |
| MCP Server | Node.js, TypeScript, @modelcontextprotocol/sdk |
| Bot Telegram | grammY |
| IA Local | Ollama |
| Build Backend | Maven |
| Build Frontend | npm |
| ORM | Hibernate (backend) |
