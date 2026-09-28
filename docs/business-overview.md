# Fintrack — De qué va esto (y cómo está armado)

> Documento de negocio, sin tecnicismos. Para releer cuando me pierda y recordar qué es cada cosa.
> Los detalles técnicos (código, tablas, endpoints) están en [`architecture.md`](architecture.md). El diagrama de entidades en [`er-diagram.md`](er-diagram.md).

---

## 1. De qué va esto

Fintrack es una app de **finanzas personales**. La idea es simple: tener en un solo lugar el control de toda la plata, para responder preguntas como:

- ¿Cuánta plata tengo en total?
- ¿En qué se me va la plata cada mes?
- ¿Cuánto me falta para cumplir una meta de ahorro?
- ¿Qué vence esta semana (tarjeta, monotributo, alquiler)?
- ¿A quién le debo plata (gastos compartidos)?

Va a tener 3 formas de usarse (más adelante):
1. Una **web** (Next.js).
2. Un **bot de Telegram** al que le escribo en lenguaje normal ("Gasté 5000 en el súper").
3. Un **server MCP** que expone herramientas para que una IA las use.

Hoy está avanzado solo el **backend** (la API en Java). El frontend y el bot todavía no están.

---

## 2. Los conceptos importantes (las "cosas" que guardo)

Cada concepto es una "entidad": algo que se guarda en la base de datos. Todo, absolutamente todo, pertenece a un **usuario**.

### Usuario (User)
Yo, que me registro con email y contraseña. Es el dueño de todo lo que sigue. Nadie ve lo de los demás.

### Cuenta (Account)
**Dónde está guardada la plata.** No es solo "banco": es cualquier lugar físico o lógico donde tengo plata.
- "Efectivo" (plata en el bolsillo)
- "Mercado Pago" (billetera digital)
- "Santander Caja de Ahorro" (banco)
- "Visa Santander" (la tarjeta de crédito, que se usa como cuenta)

Cada cuenta tiene un **titular** (owner), que es a nombre de quién está, y se puede "dar de baja" sin borrar la historia.

### Categoría (Category)
**Cómo clasifico los movimientos** para entender en qué gasto o de dónde sale la plata.
- "Sueldo" (ingreso)
- "Alquiler" (egreso)
- "Supermercado" (egreso)

Cada categoría es de un tipo: **ingreso** o **egreso**, y tiene un color para los gráficos.

### Transacción (Transaction)
**Un movimiento de plata puntual.** Lo más importante de todo:
- Entró plata (ingreso): "Cobré el sueldo, $500.000".
- Salió plata (egreso): "Compré en el súper, $25.000".

Siempre está ligada a una **cuenta** (de cuál cuenta salió/entró) y a una **categoría** (de qué tipo es).

### Gasto Fijo (FixedExpende)
**Un gasto que se repite cada mes**, predecible: alquiler, internet, Netflix, gimnasio. Tiene un **día de vencimiento** y se descuenta de una cuenta. Sirve para saber qué está por vencer.

### Tarjeta (Card)
**Mi tarjeta de crédito**, con su titular, cuánto tengo que pagar y cuándo vence. Es distinto de una cuenta: acá lo importante es **cuándo vence**.

### Deuda (Debt)
**Algo que debo y voy pagando de a poco** (un préstamo, cuotas). Guardo cuánto era el total, cuánto me queda y en qué estado está: **activa** o **pagada**.

### Meta de Ahorro (SavingGoal)
**Una meta de plata que quiero juntar**: "Viaje a Europa", "fondo de emergencia". Guardo cuánto quiero (objetivo), cuánto llevo (actual) y para cuándo. El progreso es `actual / objetivo`.

### Monotributo (Monotributo)
**El pago mensual del monotributo** (para autónomos). Guardo cuánto pago, cuándo vence y si estoy **al día** o **con deuda**.

---

## 3. Las relaciones que debo recordar (cómo se conectan)

- Un **usuario** tiene muchas cuentas, categorías, transacciones, gastos fijos, tarjetas, deudas, metas y monotributos. (Todas las entidades → apuntan a un usuario.)
- Una **transacción** apunta a **una cuenta** (de dónde) y **una categoría** (de qué es).
- Un **gasto fijo** también apunta a **una cuenta** y **una categoría**.

La regla de oro: **todo cuelga del usuario.** Una transacción tuya jamás aparece en lo de otro.

---

## 4. Reglas y decisiones de negocio (para no re-pensarlas)

1. **Soft delete**: cuando "borro" una cuenta o categoría, no se borra de verdad. Se marca como `inactiva` para no romper el historial de transacciones viejas.
2. **Balance de una cuenta** = lo que entró − lo que salió de esa cuenta. (Se va a implementar con los reportes.)
3. **"Dar de baja" no es lo mismo que "eliminar"**: desactivo, no borro.
4. **El listado de cuentas muestra solo las activas.** El de categorías muestra todas (por ahora).
5. **Card vs Debt vs Account(tipo CARD)** — fácil de confundir:
   - `Account` tipo CARD: "uso la tarjeta como si fuera una cuenta donde gasto".
   - `Card`: los **datos de la tarjeta** (titular, vencimiento, cuánto a pagar).
   - `Debt`: una **deuda** (préstamo/cuotas), no una tarjeta.

---

## 5. Cómo se conforma la base de datos (resumen no técnico)

- Motor: **PostgreSQL**. La app (Java) se encarga de crear y actualizar las tablas **sola** a partir de las entidades (Hibernate). No hay que escribir tablas a mano.
- Hay **una tabla por entidad**: `users`, `account`, `category`, `transaction`, `fixed_expende`, `card`, `debt`, `saving_goal`, `monotributo`, más dos de soporte (`refresh_token`, `password_reset_token`).
- Las conexiones se logran con claves: cada tabla tiene una columna `user_id` que apunta al usuario dueño. Ej: la tabla `transaction` tiene `user_id`, `category_id` y `account_id`.
- Los tipos con valores fijos (ingreso/egreso, cash/wallet/bank/card, al día/con deuda) se guardan como texto legible (enums).
- **No hay seeders ni datos iniciales**: la base arranca vacía; los datos se crean al usar la app.

---

## 6. Qué falta (estado del proyecto)

**Hecho (backend):**
- Autenticación completa (registrarse, loguearse, refrescar sesión, recuperar contraseña).
- Cuentas y categorías: crear, listar, editar, dar de baja.

**Por hacer:**
- Backend: transacciones, gastos fijos, tarjetas, deudas, metas, monotributo + reportes.
- Frontend web.
- Bot de Telegram + IA.
- Gastos compartidos (dividir gastos entre personas).

---

## 7. Diccionario rápido (por si me pierdo)

| Término | Qué es |
|---------|--------|
| Cuenta | Dónde está la plata |
| Categoría | Cómo clasifico un movimiento (ingreso/egreso) |
| Transacción | Un movimiento de plata (ingreso o egreso) |
| Gasto fijo | Gasto que se repite cada mes |
| Tarjeta | Tarjeta de crédito + vencimiento |
| Deuda | Algo que debo y voy pagando |
| Meta de ahorro | Objetivo de plata a juntar |
| Monotributo | Pago mensual del monotributo |
| Soft delete | Marcar inactivo en vez de borrar |
| Titular (owner) | A nombre de quién está la cuenta |
