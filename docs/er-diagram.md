# Fintrack — Diagrama Entidad-Relación

Diagrama completo en [`er-diagram.mmd`](er-diagram.mmd) (formato Mermaid puro, listo para mermaid.live o `mmdc`).

Se renderiza también en GitHub y en VS Code con la extensión "Markdown Preview Mermaid Support".

## Diagrama completo

```mermaid
erDiagram
    %% --- Relaciones (implementadas) ---
    users ||--o{ account : "tiene"
    users ||--o{ category : "tiene"
    users ||--o{ transaction : "registra"
    users ||--o{ fixed_expende : "tiene"
    users ||--o{ card : "tiene"
    users ||--o{ debt : "tiene"
    users ||--o{ saving_goal : "tiene"
    users ||--o{ monotributo : "tiene"
    users ||--o{ refresh_token : "posee"
    category ||--o{ transaction : "clasifica"
    account ||--o{ transaction : "origen"
    category ||--o{ fixed_expende : "clasifica"
    account ||--o{ fixed_expende : "origen"

    %% --- Relaciones (planificadas - gastos compartidos) ---
    users ||--o{ shared_expense : "registra"
    users ||--o{ group : "tiene"
    users ||--o{ settlement : "registra"
    group ||--o{ shared_expense : "agrupa"
    category |o--o{ shared_expense : "categoriza"
    account |o--o{ shared_expense : "paga desde"
    shared_expense ||--o{ share_participant : "incluye"
    shared_expense ||--o{ settlement : "liquidada por"

    users {
        UUID id PK
        string email UK
        string username UK
        string password
        enum rol
    }
    account {
        UUID id PK
        string name
        enum type
        string owner "nullable"
        boolean active
        UUID user_id FK
    }
    category {
        UUID id PK
        string name
        string color
        enum type
        boolean active
        UUID user_id FK
    }
    transaction {
        UUID id PK
        enum type
        string description
        decimal amount
        date date
        string notes
        UUID category_id FK
        UUID account_id FK
        UUID user_id FK
    }
    fixed_expende {
        UUID id PK
        string name
        decimal amount
        date due_day
        string frequency
        boolean active
        UUID category_id FK
        UUID account_id FK
        UUID user_id FK
    }
    card {
        UUID id PK
        string holder_name
        date due_day
        decimal amount
        boolean active
        UUID user_id FK
    }
    debt {
        UUID id PK
        string creditor
        decimal total_amount
        decimal remaining_amount
        date start_date
        enum status
        UUID user_id FK
    }
    saving_goal {
        UUID id PK
        string name
        decimal target_amount
        decimal current_amount
        date target_date
        boolean active
        UUID user_id FK
    }
    monotributo {
        UUID id PK
        string name
        decimal monthly_amount
        date due_day
        enum status
        UUID user_id FK
    }
    refresh_token {
        bigint id PK
        string hashed_token UK
        boolean revoked
        datetime fecha_expiracion
        UUID usuario_id FK
    }
    password_reset_token {
        string token PK
        string email
        bigint expiration_time
        boolean used
    }
    shared_expense {
        UUID id PK
        string description
        decimal total_amount
        date date
        string paid_by
        enum split_method
        UUID category_id FK
        UUID account_id FK
        UUID group_id FK
        UUID user_id FK
    }
    share_participant {
        UUID id PK
        string name
        decimal amount
        decimal percentage
        UUID shared_expense_id FK
    }
    settlement {
        UUID id PK
        string from_name
        string to_name
        decimal amount
        date date
        UUID shared_expense_id FK
        UUID user_id FK
    }
    group {
        UUID id PK
        string name
        UUID user_id FK
    }
```

---

## Resumen de relaciones

| Entidad A | Cardinalidad | Entidad B | Descripción |
|-----------|--------------|-----------|-------------|
| `users` | 1—N | `account` | Un usuario tiene varias cuentas |
| `users` | 1—N | `category` | Un usuario tiene varias categorías |
| `users` | 1—N | `transaction` | Un usuario registra varias transacciones |
| `users` | 1—N | `fixed_expende` | Un usuario tiene varios gastos fijos |
| `users` | 1—N | `card` | Un usuario tiene varias tarjetas |
| `users` | 1—N | `debt` | Un usuario tiene varias deudas |
| `users` | 1—N | `saving_goal` | Un usuario tiene varias metas de ahorro |
| `users` | 1—N | `monotributo` | Un usuario tiene varios registros de monotributo |
| `users` | 1—N | `refresh_token` | Un usuario tiene varios refresh tokens |
| `category` | 1—N | `transaction` | Una categoría clasifica varias transacciones |
| `account` | 1—N | `transaction` | Una cuenta es origen de varias transacciones |
| `category` | 1—N | `fixed_expende` | Una categoría clasifica varios gastos fijos |
| `account` | 1—N | `fixed_expende` | Una cuenta debita varios gastos fijos |

> `password_reset_token` es una entidad aislada: referencia al usuario por `email` (string), sin FK a `users`.

### Planificado (Fase 1.8 — Gastos Compartidos)

| Entidad A | Cardinalidad | Entidad B | Descripción |
|-----------|--------------|-----------|-------------|
| `users` | 1—N | `shared_expense` | Un usuario registra gastos compartidos |
| `users` | 1—N | `group` | Un usuario tiene grupos de personas |
| `users` | 1—N | `settlement` | Un usuario registra liquidaciones |
| `group` | 1—N | `shared_expense` | Un grupo agrupa gastos (opcional) |
| `category` | 0—N | `shared_expense` | Categoriza un gasto compartido (opcional) |
| `account` | 0—N | `shared_expense` | Cuenta desde la que se pagó (opcional) |
| `shared_expense` | 1—N | `share_participant` | El gasto incluye N participantes |
| `shared_expense` | 1—N | `settlement` | El gasto se liquida con N pagos |

---

## Enumeraciones

| Enum | Valores |
|------|---------|
| `AccountType` | `CASH`, `WALLET`, `BANK`, `CARD` |
| `CategoryType` | `INCOME`, `EXPENSE` |
| `TransactionType` | `INCOME`, `EXPENSE` |
| `DebtStatus` | `PAID`, `ACTIVE` |
| `MonotributoStatus` | `UP_TO_DATE`, `DEBT` |
| `Rol` | `USER`, `ADMIN` |
| `SplitMethod` *(planificado)* | `EQUAL` (50/50), `PERCENTAGE`, `FIXED` |
