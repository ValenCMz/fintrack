# Deuda de testing

Estado al 2026-10-06: 4 de los 5 ítems que dolían están resueltos y verificados por
tests. Quedan dos ítems que **no se arreglan con tests** (locking optimista) y lo que
se decidió no cubrir.

El checklist de lo que hay que patear a mano está en `testing-manual.md`.
La suite pasó a **127 unitarios + 6 tests de integración** (antes 86). No se rompió
nada con el nuevo `@Digits`/`@Size`: los datos de la base y el frontend ya respetaban
los límites, así que el `PATCH` de optimización (que actualiza los DTO a la columna)
quedó desbloqueado.

## 1. JWT y seguridad de la cadena de tokens — resuelto ✅

El plan anunciaba `JwtServiceKeyTest` y `SecurityConfigIT` y no existían. Ahora:

- `JwtServiceKeyTest` (unitario, usa `SpringShell` + RSA válida en loop): arranca con
  claves válidas, revienta el contexto con inválida y corta. Cubre también
  separador de refresh/access, expiración y que firmar de verdad verifica.
- `SecurityConfigIT` (Spring Security, sin Docker): `GET /users` con `USER` → `403`,
  con `ADMIN` → `200`.
- `RefreshTokenServiceTest`: refresh no encontrado/vencido/revocado → `401`,
  reutilizado → rechazado y se revoca la cadena, token de otro usuario → `401`.
- `UsuarioServiceTest` extendido: forgot password genérico y **resetPassword** (el
  bug que quedó anotado en `endpoints.md` — token usado re-servía — quedó cerrado:
  ver abajo).

## 2. Límites de tamaño de los montos — resuelto ✅

`@Digits(integer = 36, fraction = 2)` en los 8 DTOs de dinero, alineado a la
`numeric(38,2)` real de la base. Antes un `amount` de 37 dígitos caía como `409`
sucio del `DataIntegrityViolationException` sin clasificar; ahora es un `400` limpio
del bean validation. Test de 3 decimales y de 37 dígitos. Un monto de 36 enteros +
2 decimales sigue entrando (tope de la columna, verificado).

Verificado con la base que los límites viejo/nuevo son iguales: el mensaje ahora
dice 36 enteros, que era lo que la columna ya permitía.

## 3. Concurrencia — documentado (no arreglado: es deuda de código, no de testing)

Sigue sin `@Version` → dos `PUT` se pisan y gana el último en silencio. **No se
arregló** (cambiar a locking optimista es otra tarea, con implicancias de UI: hay que
mostrar el conflicto).

Lo que sí se hizo por la deuda de testing: `OptimisticLockingTest` documenta el
comportamiento actual (el update deja de afectar filas sin aviso, "lost update").
Cuando se agregue `@Version`, **este test tiene que cambiar** por el que espera un
conflicto/optimistic lock exception. Queda anotado ahí dentro.

## 4. Longitudes y unicode — resuelto ✅

- `@Size` en los strings que iban a una columna con límite real, con el máximo de la
  columna: `name` (cuentas, categorías, etc.) → 7.959, `description` 255, `notes`
  1000, `color` 30, `frequency` 50, `email` 254, `username` 50, password ≤72 (bcrypt
  trunca a 72 **bytes**, ojo con contraseñas con caracteres multibyte).
- Test de un nombre de 10.000 chars rechazado y de nombres unicode/acentos/emoji
  aceptados (`@Pattern` con `[\\p{L} ]` ya existía en algunos).

> Ojo que queda una diferencia sutil sin cubrir: `password` tiene `@Size(max=72)` por
> **chars**, bcrypt corta por **bytes**. Un password de 72 chars "ñ" (144 bytes) aún
> podría truncarse mal. En la práctica es inviable y se dejó así.

- La columna `name` sigue sin límite en la base; el `@Size` del DTO es lo que manda.

## 5. tests dependientes del reloj — resuelto ✅

`ReportServiceProjections` ahora recibe un `Clock` inyectado (por constructor), el
test usa un reloj fijado y las proyecciones son deterministas. Se agregó además un
test que cruza el mes **a propósito** (último día 23:59 vs primer día 00:00 con
el histórico fijo), que antes era imposible sin reescribir el servicio. Las proyecciones
de 1 y 12 meses, el promedio conservador (3 meses) y el historial vacío quedaron
cubiertos.

## 6. Registrado para cuando toque (no arreglado)

- **Cantidades en `PATCH /users`**: el nuevo `@Size` válida con el user `null` y tira
  `@NotNull`. Todas las columnas que tocan host en la base real son `not null`, así
  que nunca hubo un `null` válido. Se espera que navegadores/crawlers puedan mandar
  campos vacíos → `400`. Es un salto de comportamiento documentado, no un bug.
- **Locking optimista** (ítem 3): implementarlo es tarea de código + UI.
- **Bug conocido de confort**: `POST /auth/refreshToken` sin la cookie da `500` en vez
  de `401` (`MissingRequestCookieException` sin manejar) — anotado en `endpoints.md`.
- **Tooling**: `AccountRepositoryIT` levanta su Postgres en un `static` block, así que
  sin Docker revienta al cargar la clase y no se "saltea" solo. Correrlo con Docker.

## 7. Lo que se decidió no cubrir

Igual que en `testing-plan.md`: carga y rendimiento, E2E de UI, mails reales, y
MCP/Telegram/NLP. Lo último entra en alcance si llega el agente de Telegram.