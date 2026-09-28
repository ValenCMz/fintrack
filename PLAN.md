# Fintrack — Plan de Implementación

## MVP vs Post-MVP

```
┌─ MVP ─────────────────────────────────────────────┐
│ Fase 1: Backend REST API (Java / Spring Boot)     │
│ Fase 2: Frontend Web App (Next.js / React)        │
│ Fase 3: MCP Server + Telegram Bot + NLP           │
└────────────────────────────────────────────────────┘
┌─ Post-MVP ─────────────────────────────────────────┐
│ Fase 4: IA Local (Ollama) — opcional              │
│ Fase 5: Infraestructura y DevOps                  │
└────────────────────────────────────────────────────┘
```

---

## Arquitectura General

```
                              ┌─────────────┐
                              │  API IA     │
                              │  (Gemini /  │
                              │  DeepSeek)  │
                              └──────┬──────┘
                                     │ NLP (procesamiento de mensajes)
PostgreSQL ← Java Spring Boot ←┬─┐   │
  (REST API)                   │ ├───┘
                               │ │
                               │ └─ Node.js MCP Server ←── Telegram Bot
                               │
                               └─── Next.js Frontend (Web App)
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

### 1.8 Gastos Compartidos
Entidades nuevas para gestionar gastos entre personas (pareja, convivencia, salidas con amigos):
- `SharedExpense` — gasto compartido: quién pagó, monto total, categoría, fecha, descripción
- `ShareParticipant` — cada persona que participa del gasto (nombre/alias) y cuánto le corresponde
- `Group` — grupo de personas (opcional para MVP, ej: "Casa", "Viaje a la costa")

Modelo de división:
- **50/50**: monto total dividido en partes iguales entre los N participantes
- **Porcentaje**: cada participante asume un % del total (ej: 60/40 en convivencia)
- **Monto fijo**: a cada participante se le asigna un monto específico (ej: uno pagó entrada $5000, otro pagó comida $3000, etc.)

Funcionalidades:
- **Crear gasto compartido**: definir participantes, quién pagó, cómo se divide
- **Registrar pago/liquidación**: cuando un participante le paga al que adelantó
- **Balance entre personas**: calcular quién le debe a quién y cuánto (net balance)
- **Historial de gastos compartidos**: filtros por grupo, persona, fecha
- **Vincular a cuentas/categorías**: un gasto compartido genera una transacción real en la cuenta del pagador

Repositorios:
- `SharedExpenseRepository`
- `ShareParticipantRepository`
- `SettlementRepository` (pagos de liquidación entre participantes)

DTOs:
- `SharedExpenseRequest`, `SharedExpenseResponse`
- `ShareParticipantDto`
- `SettlementRequest`
- `BalanceResponse` (resumen de deudas: quién debe a quién)

Servicios:
- `SharedExpenseService` — CRUD + cálculo de balances + liquidaciones

Endpoints REST:

| Recurso | Endpoints |
|---------|-----------|
| Shared Expenses | `GET/POST /api/shared-expenses`, `GET/PUT/DELETE /api/shared-expenses/{id}` |
| Balances | `GET /api/shared-expenses/balances` — saldos entre personas |
| Settlements | `POST /api/shared-expenses/{id}/settle` — registrar pago de deuda |
| Groups | `GET/POST /api/groups`, `GET /api/groups/{id}/expenses` (opcional MVP) |

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

### 2.7 Gastos Compartidos
- **Página** (`/gastos-compartidos`): listado de gastos compartidos con filtros
- **Formulario de nuevo gasto**: seleccionar quién pagó, participantes (multi-select o input libre de nombres), monto, cómo se divide (50/50, %, fijo), categoría, descripción, fecha
- **Vista de balances**: pantalla que muestra deudas netas entre personas (estilo "Vos le debés $X a María", "Juan te debe $Y")
- **Liquidación**: botón "Marcar como pagado" cuando alguien salda una deuda
- **Widget en Dashboard**: resumen rápido de balances pendientes
- **Componentes**:
  - `SharedExpenseCard` — tarjeta de gasto compartido
  - `BalanceSummary` — resumen visual de deudas
  - `ParticipantSelector` — selector de participantes
  - `SplitMethodSelector` — toggle entre 50/50, porcentaje, monto fijo

---

## Fase 3: MCP Server + Telegram Bot + NLP (Node.js / TypeScript)

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

### 3.4 NLP con API Externa (Gemini Flash / DeepSeek)
- Procesamiento de lenguaje natural para mensajes sin comando (ej: "Gasté 5000 en supermercado") y consultas conversacionales
- Usar API externa de IA para interpretar intención y extraer parámetros:
  - **Gemini Flash**: tier gratuito generoso (~1500 req/día), excelente en español
  - **DeepSeek**: ~0.14 USD / 1M tokens input, extremadamente barato, calidad sólida
- Flujo: mensaje del usuario → API IA interpreta intención → MCP ejecuta tool → respuesta formateada
- Funcionalidades NLP en el bot:
  - Registrar transacciones en lenguaje natural ("Pagué 2000 de luz", "Cobré 50000 del laburo")
  - Consultas conversacionales ("¿Cuánto gasté este mes en comida?", "¿Cómo voy con el ahorro?")
  - Consejos financieros y análisis de patrones de gasto
  - Resúmenes y reportes en lenguaje natural
- El costo mensual para uso personal/familiar es despreciable (centavos de dólar)

### 3.5 Chat en Frontend (`/chat`)
- Conectar la página `/chat` al endpoint de NLP del MCP server
- El chat envía el historial de transacciones y metas como contexto
- Soporte para streaming de respuestas (Server-Sent Events)
- Mismas capacidades NLP que el bot de Telegram

### 3.6 Autenticación del Bot
- Vincular cuenta de Telegram con usuario de la app (web o vía bot)
- Almacenar mapping `chatId → userId`

### 3.7 Gastos Compartidos vía MCP y Telegram
Herramientas MCP adicionales para gastos compartidos:

| Tool | Descripción |
|------|-------------|
| `add_shared_expense` | Registrar un gasto compartido entre personas |
| `list_shared_expenses` | Listar gastos compartidos con filtros |
| `get_balances` | Ver quién le debe a quién y cuánto |
| `settle_up` | Registrar pago de liquidación entre personas |

Comandos del bot de Telegram:
- `/compartir <monto> <descripción> con <personas>` — registrar gasto compartido
- `/saldo` — ver balances pendientes (quién te debe / a quién le debés)
- `/liquidar <persona> <monto>` — registrar que pagaste una deuda

NLP para gastos compartidos:
- "Pagué la cena de anoche, $12000 entre Juan, María y yo"
- "Dividimos el alquiler $80000, yo pagué 60% y Ana 40%"
- "¿Cuánto me debe Juan?"
- "Le pagué $5000 a María por lo del finde"

---

## Fase 4: IA Local (Ollama) — Post-MVP, Opcional

- Migrar el pipeline de NLP de API externa a Ollama local
- Relevante si el proyecto escala y el costo de API externa deja de ser trivial
- Elegir modelo (ej: `llama3.2`, `qwen2.5`)
- El MCP server expone el mismo endpoint de NLP, solo cambia el provider
- Ventajas: sin dependencia de terceros, sin costo por request
- Desventajas: requiere GPU o servidor con buena RAM, latencia mayor en CPU
- Mejoras adicionales opcionales:
  - RAG con PDFs de extractos bancarios
  - Integración con APIs de cotización (dólar, crypto)
  - Recomendaciones de inversión simples

---

## Fase 5: Infraestructura y DevOps — Post-MVP

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
| NLP (MVP) | Gemini Flash o DeepSeek (API externa) |
| NLP (Post-MVP) | Ollama (local) |
| Build Backend | Maven |
| Build Frontend | npm |
| ORM | Hibernate (backend) |
