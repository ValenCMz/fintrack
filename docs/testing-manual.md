# Prueba manual — Checklist

Paseo manual de la app antes de release. Lo que los tests no cubren (o cubren a
un nivel distinto). No hay runner de frontend, así que esto es la caja negra de
la UI + los casos de seguridad que conviene tocar con `curl` y ver con los
propios ojos.

> Pre-requisito asumido: backend en `:8080`, frontend en `:3000`, y una cuenta
> `yo@test.com` (con también una cuenta `admin@test.com` con rol `ADMIN`).

---

## 1. Autenticación

- [ ] `POST /auth/register` con `{"username":"valentin","email":"yo@test.com","password":"Test1234!"}` da `200` y la cookie/response correctos
- [ ] Repetir el mismo email da `400` "Ya existe un usuario con ese email"
- [ ] Repetir el mismo **username** da `409` (llega al UNIQUE de la base, distinto del email)
- [ ] Password de 7 caracteres da `400`; password de 8 sin mayúscula ni número da `400`
- [ ] Password de más de 72 caracteres da `400` (**nuevo** con `@Size(max=72)`; bcrypt trunca a 72 bytes)
- [ ] Username de más de 50 caracteres da `400` (**nuevo**); email de más de 254 da `400`
- [ ] Login con credenciales malas da `401` *"Credenciales inválidas"* (igual para email inexistente que para password mal, no se puede enumerar)
- [ ] `GET /users/me` devuelve el usuario del token
- [ ] Login setea cookie `refreshTokenFinTrack` con `HttpOnly`, `Path=/api/auth/refreshToken`, `Max-Age=2592000` (30 días)

### Refresh token (lo más sensible)

- [ ] `POST /auth/refreshToken` con la cookie devuelve un access token nuevo y **rota** el refresh (una segunda llamada con el mismo cookie/sesion ya no anda → `401`)
- [ ] **Bug conocido**: sin la cookie responde `500` (`MissingRequestCookieException` sin manejar). Debería ser `401`. Anotado en endpoints.md
- [ ] Hacer logout e intentar refreshar → `401` (tokens revocados)

### Forgot / reset password

- [ ] Email inexistente → `200` con el mensaje genérico (no revela si existe)
- [ ] Email mal formado → `400`
- [ ] Email existente → llega el mail (catcher SMTP) con el link `/reset-password?token=...`
- [ ] Reset con token válido y password nueva → `200`, obliga a reloguear (se revocaron los refreshes)
- [ ] Reusar el **mismo** token ya usado → `400` (**bug corregido**: antes el token usado servía de nuevo dentro de los 15 min)
- [ ] Token vencido (>15 min) → `400`; token de otro email → `400`

## 2. Roles y propiedad de los datos

- [ ] `GET /users` con token `USER` → `403`; con token `ADMIN` → `200`
- [ ] `PUT /users/{id}` de otro usuario → `403`; del propio → `200`
- [ ] `USER` mandando `rol` → `403`; `ADMIN` cambiando rol de otro → `200`
- [ ] `PUT /users/{id}` con email inválido o duplicado → `400`
- [ ] `PUT /users/{id}` con username o email de más de 50/254 chars → `400` (**nuevo**)
- [ ] Endpoint de un recurso **de otro usuario** → `404` (no distingue a propósito)

## 3. Validación de montos (numeric(38,2))

Crear con `curl` las transacciones/tarjetas y ver el status.

- [ ] `amount` con 3 decimales (`"80000.123"`) → `400` (**nuevo** con `@Digits`)
- [ ] `amount` de 37 dígitos enteros → `400` (**nuevo**; antes caía como `409` sucio de la base)
- [ ] Tope de la columna: 36 enteros + 2 decimales → `201`
- [ ] `totalAmount`/`targetAmount`/`amount` en `0` o negativo → `400`
- [ ] `remainingAmount`/`currentAmount` en `0` → válido (`201`)
- [ ] `amount` null → `400`

## 4. Validación de textos

- [ ] Nombre de cuenta/tarjeta/meta de 10.000 chars → `400` (**nuevo** con `@Size`)
- [ ] `description` de transacción con 255 chars entra, 256 → `400`; `notes` >1000 → `400`
- [ ] `color` de categoría de más de 30 chars → `400`; `frequency` de más de 50 → `400`
- [ ] Nombres con **acentos, emojis y CJK** (`"Cuénta 🧉 日本語 Vélez"`) entran sin problema

## 5. CRUD y baja lógica vs borrado real

Para **accounts, categories, cards, saving-goals, fixed-expenses** (baja lógica):

- [ ] Crear → `201`
- [ ] `DELETE` → `200`; el listado **ya no lo muestra**
- [ ] `GET /{id}` **sigue devolviéndolo** con `active: false`
- [ ] `PUT` con `active: true` lo reactiva y vuelve al listado

Para **transactions, debts, monotributo** (borrado real):

- [ ] `DELETE` → `200`; `GET /{id}` ahora da `404`

- [ ] En el frontend: borrar/desactivar hace desaparecer el registro de la lista **sin recargar la página**

## 6. Tarjetas y cuotas

- [ ] Comprar en tarjeta con `installments` 1-36 → `201`, y `GET /cards/{id}/detail`
      devuelve N cuotas; la suma de las cuotas es **exactamente** el monto (resto en la última)
- [ ] `outstanding` = suma de cuotas impagas; al pagar la última cuota queda `0`
- [ ] Pagar una cuota con `POST /cards/{id}/installments/{iid}/pay` → la cuota pasa a `paid`
      y `GET /reports/balance` descuenta el importe de la cuenta elegida
- [ ] Pagar el mismo cuota dos veces, o un detalle ya saldado → `400`
- [ ] `POST /cards/{id}/items/{tid}/pay` salda **todas** las cuotas impagas del detalle
- [ ] Cambiar `amount` u `installments` de una compra con cuotas pagas → `400`
- [ ] `PUT /transactions/{id}` con `installments: 0` o `37` → `400`
- [ ] Borrar una compra de tarjeta borra sus cuotas y sus pagos, y el balance de la cuenta vuelve

## 7. Reportes

- [ ] `GET /reports/monthly-summary` sin params usa el mes actual; con `?year=&month=` usa el pedido
- [ ] `GET /reports/by-category?from=&to=` agrupa egresos por categoría
- [ ] `GET /reports/balance` descontar saldos por cuenta
- [ ] `GET /reports/projections?months=1` y `months=12`; fuera de rango (`0`, `13`, letras) → `400` *"months debe estar entre 1 y 12"*
- [ ] Sin movimientos → `[]` (no ceros)
- [ ] Con movimientos de **un solo** mes → promedio = total/3 (conservador)
- [ ] El mes en curso **no** contamina el promedio de los 3 cerrados

## 8. Seguridad de la sesión en navegador

- [ ] Token vencido (1 h) → el frontend redirige a `/login` sin crashear
- [ ] Manipular el access token (cambiar un carácter) → `401`, no `500`
- [ ] Sesión de otro usuario en otra pestaña no mezcla datos (aceite en el `localStorage`)

## 9. Frontend — paseo en navegador

- [ ] `/login` → login ok entra al dashboard; logout saca
- [ ] `/` dashboard: stats reales, transacciones recientes y **proyección** con los meses esperados
- [ ] `/ingresos` y `/egresos`: listado real con filtros por fecha/categoría
- [ ] `/cuentas`, `/categorias`: crear, editar, desactivar, ver la lista
- [ ] `/tarjetas`, `/deudas`, `/ahorros`, `/gastos-fijos`: CRUD real
- [ ] `/monotributo` ya no existe en el frontend (solo API); `/tarjetas/{id}/detalle` abre el diálogo de cuotas
- [ ] `/settings`: perfil y preferencias
- [ ] `/chat`: placeholder (Fase 3 no implementada)
- [ ] Los errores de la API llegan a la pantalla (mensaje visible, no tragado)
- [ ] Un recurso desactivado desaparece de la lista sin refrescar

## 10. Concurrencia (comportamiento conocido, no es bug nuevo)

- [ ] Dos pestañas editando la misma cuenta guardan las dos → gana la última en
      silencio. **No** hay conflicto ni aviso (no hay `@Version`). El test
      `OptimisticLockingTest` documenta esto; cuando se agregue locking optimista,
      ese test debe cambiar y el usuario debería ver el conflicto.

## 11. Suite automatizada

```bash
# Unitarios + SecurityConfigIT (sin Docker). SecurityConfigIT corre en verify.
cd fintrack && mvn -o test          # 134 unitarios
cd fintrack && mvn -o verify -Dit.test=SecurityConfigIT

# Integración con PostgreSQL real (requiere Docker)
cd fintrack && mvn -o verify -Dit.test='!AccountRepositoryIT'

# Frontend
cd fintrack-frontend && npx tsc --noEmit && npm run lint && npm run build
```

> Nota de tooling: `AccountRepositoryIT` levanta su contenedor en un bloque
> `static`, así que sin Docker revienta al cargar la clase y no se "saltea".
> Correrlo únicamente en máquinas con Docker.