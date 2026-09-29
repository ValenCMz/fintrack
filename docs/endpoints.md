# API — Referencia de Endpoints

Referencia de la API HTTP de FinTrack, para probarla endpoint por endpoint con `curl`.

Todo sale del código: controllers, DTOs, entidades y `SecurityConfig`. Si algo no coincide
con el código, el código tiene razón y esta doc está mal.

---

## Base

| Concepto | Valor |
|----------|-------|
| Prefijo | `/api` (viene de `server.servlet.context-path`, no de los controllers) |
| Puerto por defecto | `8080` |
| URL base | `http://localhost:8080/api` |
| Content-Type | `application/json` |
| Formato de fechas | `YYYY-MM-DD` (ISO) |
| Formato de importes | número decimal, sin símbolo. `"amount": 1500.50` |

## Autenticación

Todos los endpoints salvo `/auth/**` requieren un JWT.

```bash
# Login: devuelve el accessToken en el body
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"yo@test.com","password":"Test1234!"}'

# Guardar el token para los ejemplos siguientes
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"yo@test.com","password":"Test1234!"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")

# Usarlo
curl -s http://localhost:8080/api/reports/balance -H "Authorization: Bearer $TOKEN"
```

El login además deja un refresh token en la cookie httpOnly `refreshTokenFinTrack`, que
es lo único que usa `POST /auth/refreshToken`. El access token va en el body, y el
frontend lo guarda en `localStorage`.

## Envelope

Toda respuesta, éxito o error, tiene la misma forma.

```json
{ "data": { }, "success": true }
```

```json
{ "error": { "status": 404, "message": "Account not found" }, "success": false }
```

`message` en errores de validación viene con el nombre del campo:
`"amount: no debe ser nulo"`.

## Códigos de estado

| Código | Cuándo |
|--------|--------|
| `200` | Lecturas y updates. También `POST /auth/login` y `POST /auth/register` |
| `201` | Creaciones de recursos (`/accounts`, `/categories`, `/transactions`, `/cards`, `/debts`, `/saving-goals`, `/monotributo`, `/fixed-expenses`) |
| `400` | Body o query params inválidos |
| `401` | Sin token, token vencido o credenciales malas |
| `403` | Token válido pero sin permiso (recurso de otro usuario, o rol insuficiente) |
| `404` | El recurso no existe **o es de otro usuario** — no se distingue a propósito |
| `409` | Conflicto de constraint en la base (duplicado, campo obligatorio, FK) |

> Los `POST` de auth devuelven `200` y los de recursos devuelven `201`. No es uniforme,
> está así a propósito para no romper el frontend.

---

# Autenticación — `/auth`

Acceso público, sin token.

### `POST /auth/register`

```json
{
  "username": "valentin",
  "email": "yo@test.com",
  "password": "Test1234!"
}
```

| Campo | Regla |
|-------|-------|
| `username` | obligatorio, único |
| `email` | obligatorio, único, formato email |
| `password` | obligatorio, mínimo 8 caracteres, y al menos **una mayúscula o un número** (con `o`, no `y`) |

Respuesta `200`:

```json
{ "data": { "username": "valentin", "email": "yo@test.com", "rol": "USER" }, "success": true }
```

Errores: `400` por validación o contraseña débil, `409` por `username` repetido.

> El email repetido da `400` y dice `"Ya existe un usuario con ese email"`, porque el
> service lo chequea antes de insertar. El `username` repetido **no** se chequea, llega
> al UNIQUE de la base y vuelve como `409 "Ya existe un registro con ese valor"`. Los
> dos casos son el mismo problema con códigos distintos.

### `POST /auth/login`

```json
{ "email": "yo@test.com", "password": "Test1234!" }
```

Respuesta `200`:

```json
{ "data": { "accessToken": "eyJhbGciOi..." }, "success": true }
```

Además setea la cookie `refreshTokenFinTrack`.

Errores: `400` si falta un campo o el JSON está mal formado, `401` con
`"Credenciales inválidas"` si el email no existe **o** la contraseña no matchea. Los
dos casos devuelven lo mismo, así que no se puede usar el login para saber qué emails
están registrados.

### `POST /auth/refreshToken`

Sin body. Lee la cookie `refreshTokenFinTrack`. Devuelve un access token nuevo y rota el
refresh.

Errores: `401` si la cookie está vencida o revocada. **Sin la cookie responde `500`**
(`MissingRequestCookieException` no está manejado y cae en el fallback genérico). Es un
bug conocido: debería ser un `401`.

### `POST /auth/forgotPassword`

```json
{ "email": "yo@test.com" }
```

Respuesta `200` **siempre**, exista o no la cuenta:

```json
{ "data": "Si esta cuenta existe recibirás un mail", "success": true }
```

Es deliberado: confirmar el correo existiría permitiría enumerar qué emails están
registrados. Si existe, manda el mail con el link a `/reset-password?token=...`.
Errores: `400` si el email no tiene formato válido.

### `POST /auth/resetPassword`

```json
{ "resetToken": "el-token-del-mail", "newPassword": "Nueva1234" }
```

Errores: `400` con `"Error en la validacion"` si el token no existe, está vencido o no
corresponde a ese email. Ojo con el nombre del mensaje: no dice qué falló.

Al resetear se revocan todos los refresh tokens del usuario, así que hay que volver a
loguear. La respuesta `200` es `"Contraseña actualizada correctamente"`.

---

# Usuarios — `/users`

### `GET /users/me`

Devuelve el usuario del token. `200`:

```json
{ "data": { "id": "uuid", "username": "valentin", "email": "yo@test.com" }, "success": true }
```

### `GET /users`

**Solo ADMIN** (`@PreAuthorize("hasRole('ADMIN')")`). Devuelve todos los usuarios.
Un `USER` normal recibe `403`.

### `PUT /users/{id}`

PATCH semántico: los campos que vengas en `null` no se tocan.

```json
{ "username": "nuevo", "email": "nuevo@test.com", "rol": "ADMIN" }
```

Reglas:

- Un usuario solo puede modificarse a sí mismo. Otro `id` da `403`.
- Solo un ADMIN puede mandar `rol`. Un `USER` que mande `rol` da `403`.
- `email` se valida por formato y por duplicado antes de guardarse.
- `username` **no tiene validación**: se acepta cadena vacía, y un `username` repetido
  choca con el UNIQUE de la base y devuelve `409`. Es una inconsistencia conocida.

---

# Cuentas — `/accounts`

Campo clave: `type` es `CASH` | `WALLET` | `BANK` | `CARD` (en inglés, no en español).
`owner` es opcional.

```json
{ "name": "Santander Caja", "type": "BANK", "owner": "Valentin", "active": true }
```

> `active` es `boolean` primitivo. Si no lo mandás, Jackson lo deja en `false`, la cuenta
> se crea y no aparece en ningún listado. Mandá `true` explícitamente.

| Endpoint | Devuelve |
|----------|----------|
| `GET /accounts` | Lista de las activas |
| `GET /accounts/{id}` | Una, `404` si no es tuya |
| `POST /accounts` | `201` con la creada |
| `PUT /accounts/{id}` | `200` con la actualizada |
| `DELETE /accounts/{id}` | `200`, **baja lógica** (ver más abajo) |

`AccountResponse`: `{ id, name, type, owner, active }`

---

# Categorías — `/categories`

```json
{ "name": "Alquiler", "color": "#FF0000", "type": "EXPENSE", "active": true }
```

`type` es `INCOME` | `EXPENSE`. `color` es obligatorio pero no se valida el formato hex,
se guarda lo que mandes.

Mismos 5 endpoints que cuentas. `CategoryResponse`: `{ id, name, color, type, active }`

---

# Transacciones — `/transactions`

```json
{
  "type": "EXPENSE",
  "description": "Alquiler de septiembre",
  "amount": 80000.00,
  "date": "2026-09-01",
  "notes": "opcional",
  "categoryId": "uuid",
  "accountId": "uuid"
}
```

| Campo | Regla |
|-------|-------|
| `type` | `INCOME` | `EXPENSE` |
| `description` | obligatorio, no vacío |
| `amount` | obligatorio, mayor a 0 |
| `date` | obligatorio |
| `categoryId` | obligatorio, debe existir y ser tuya |
| `accountId` | obligatorio, debe existir y ser tuyo |
| `notes` | opcional, la columna es nullable |

| Endpoint | Notas |
|----------|-------|
| `GET /transactions` | Todas, sin filtro ni paginación |
| `GET /transactions/summary?from=&to=` | Totales del rango. Sin params toma todo |
| `GET /transactions/{id}` | Una |
| `POST /transactions` | `201` |
| `PUT /transactions/{id}` | `200` |
| `DELETE /transactions/{id}` | `200`, **borrado real**, la fila desaparece |

`TransactionResponse` agrega los nombres resueltos: `{ id, type, description, amount,
date, notes, categoryName, accountName }`

`TransactionSummaryResponse`: `{ totalIncome, totalExpense, balance, from, to }`

---

# Tarjetas — `/cards`

```json
{ "holderName": "Valentin C", "dueDay": "2026-09-20", "amount": 50000, "active": true }
```

`dueDay` es una **fecha**, no un número de día. Mismos 5 endpoints que cuentas.
`DELETE` es baja lógica.

`CardResponse`: `{ id, holderName, dueDay, amount, active }`

---

# Deudas — `/debts`

```json
{
  "creditor": "Banco",
  "totalAmount": 50000,
  "remainingAmount": 50000,
  "startDate": "2026-01-01",
  "status": "ACTIVE"
}
```

`status` es `PAID` | `ACTIVE`. `remainingAmount` puede ser `0` (deuda saldada),
`totalAmount` tiene que ser mayor a 0.

Mismos 5 endpoints, pero `DELETE` es **borrado real**.

`DebtResponse`: `{ id, creditor, totalAmount, remainingAmount, startDate, status }`

---

# Metas de ahorro — `/saving-goals`

```json
{
  "name": "Fondo de emergencia",
  "targetAmount": 300000,
  "currentAmount": 0,
  "targetDate": "2027-06-01",
  "active": true
}
```

`currentAmount` puede ser `0`. Mismos 5 endpoints, `DELETE` es baja lógica.

`SavingGoalResponse`: `{ id, name, targetAmount, currentAmount, targetDate, active }`

---

# Monotributo — `/monotributo`

```json
{ "name": "Cuota 09/2026", "monthlyAmount": 55000, "dueDay": "2026-09-10", "status": "DEBT" }
```

`status` es `UP_TO_DATE` | `DEBT`. Mismos 5 endpoints, `DELETE` es **borrado real**.

`MonotributoResponse`: `{ id, name, monthlyAmount, dueDay, status }`

---

# Gastos fijos — `/fixed-expenses`

```json
{
  "name": "Alquiler",
  "amount": 80000,
  "dueDay": "2026-09-10",
  "frequency": "mensual",
  "categoryId": "uuid",
  "accountId": "uuid",
  "active": true
}
```

`frequency` es **texto libre**: el backend no lo valida ni lo normaliza. Se acepta
`"mensual"`, `"quincenal"`, `"cada 15 dias"`, lo que sea. Por eso
`/reports/projections` no puede sumar los gastos fijos: no hay forma confiable de
convertirlos a un monto mensual. Los pagos igual entran al promedio histórico porque
son transacciones, no gastos fijos registrados.

Mismos 5 endpoints, `DELETE` es baja lógica.

`FixedExpendeResponse`: `{ id, name, amount, dueDay, frequency, active, categoryName, accountName }`

---

# Reportes — `/reports`

### `GET /reports/monthly-summary?year=&month=`

Sin params usa el mes actual. Respuesta:
`{ year, month, totalIncome, totalExpense, balance }`

### `GET /reports/by-category?from=&to=`

Gasto por categoría en el rango. Sin params toma todo.
Respuesta: `[{ categoryName, total }]`

### `GET /reports/balance`

Saldo por cuenta, ya descontando las transacciones.
Respuesta: `[{ accountId, accountName, balance }]`

### `GET /reports/projections?months=3`

`months` va de 1 a 12, default 3. Fuera de rango: `400` con
`"months debe estar entre 1 y 12"`.

```json
{
  "data": [
    { "month": "2026-10", "estimatedIncome": 300000.00,
      "estimatedExpense": 100000.00, "estimatedNet": 200000.00 }
  ],
  "success": true
}
```

Cómo se calcula:

- Toma los movimientos de los **3 meses cerrados** anteriores al actual.
- Divide siempre por 3, no por la cantidad de meses con datos. Con un solo mes
  cargado da "un tercio de lo que gastaste", que es la lectura conservadora.
- Aplica ese promedio constante a cada mes futuro.
- `month` viene como `"YYYY-MM"` y arranca en el mes siguiente al actual.
- Si no hay movimientos, devuelve `[]` en vez de ceros.

---

# Baja lógica vs borrado real

No todos los `DELETE` se comportan igual. Esto es importante para las pruebas:

| Recurso | `DELETE` hace | El listado después |
|---------|---------------|--------------------|
| `/accounts` | `active = false` | desaparece |
| `/categories` | `active = false` | desaparece |
| `/cards` | `active = false` | desaparece |
| `/saving-goals` | `active = false` | desaparece |
| `/fixed-expenses` | `active = false` | desaparece |
| `/transactions` | borra la fila | desaparece |
| `/debts` | borra la fila | desaparece |
| `/monotributo` | borra la fila | desaparece |

En la baja lógica, `GET /{id}` **sigue devolviendo** el recurso con `active: false`;
solo se saca del listado. Para volver a verlo hay que llamar al `PUT` con `active: true`.

La asimetría es intencional: la baja lógica conserva el historial de los pagos, que es
el dato que después alimenta las proyecciones.

> Ojo con el mensaje: el backend responde `"Tarjeta eliminada"` incluso en la baja
> lógica. El frontend dice "desactivada" por su cuenta, no usa el mensaje del backend.

---

# Recursos sin CRUD

| Endpoint | Estado |
|----------|--------|
| `/gastos-compartidos` | No existe. No hay entidad, controller ni service |
| `PUT /users/{id}` con `password` | No se puede cambiar la contraseña por acá, va por `/auth/resetPassword` |
| Paginación | No hay en ningún listado |
