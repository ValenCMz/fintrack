# Deuda de testing

Casos que la suite actual **no** cubre, ordenados por lo que duele si se escapan.
No es trabajo pendiente inmediato: está anotado para cuando toque.

## 1. JWT y seguridad de la cadena de tokens — lo más importante

El plan de testing anunciaba `JwtServiceKeyTest` y `SecurityConfigIT`, y **nunca se
escribieron**. Es lo más grave de esta lista porque el resto de la API depende de
que esta parte esté bien.

Lo que quedó sin probar:

- `JwtService.getKey()` hace `Decoders.BASE64.decode()` y arma la clave sin validar
  nada. Falta el test de que una clave inválida o demasiado corta falla al arrancar.
- La expiración del access token (1 h por defecto) nunca se cruzó con el reloj.
- El refresh token dura 30 días y se guarda hasheado con BCrypt, pero no hay test de que
  un refresh vencido, revocado o reutilizado se rechace.
- `generateResetToken` dura 15 minutos y su chequeo de revocación nunca se ejercitó.
- `GET /users` con rol insuficiente da `403`, pero eso está verificado a mano con `curl`,
  no por un test que lo proteja.

## 2. Límites de tamaño de los montos

Las 8 columnas de dinero son `numeric(38,2)`, pero **ningún DTO tiene `@Digits`**. Un
`amount` con más de 36 dígitos enteros revienta en la base y llega al
`GlobalExceptionHandler` como un `DataIntegrityViolationException` sin clasificar.

Lo que falta:

- `@Digits(integer = 36, fraction = 2)` en los 8 montos, alineado con la columna real.
- Test de que un monto con 3 decimales se rechaza o se trunca, según qué se decida.
- Test de un monto absurdamente grande.

## 3. Concurrencia

Ninguna entidad tiene `@Version`, así que no hay locking optimista. Dos `PUT`
simultáneos sobre la misma cuenta se pisan en silencio: gana el último, y el usuario no
se entera de que su edición se perdió.

Esto no se arregla con un test, se arregla con `@Version` en las entidades. Pero sí
conviene un test que **documente** la comportamiento actual, para que cuando se agregue
el locking el test cambie y quede claro.

## 4. Longitudes y unicode

- Los strings de los DTOs no tienen `@Size` (salvo `RegisterDTO`): un nombre de cuenta
  de 10.000 caracteres se guarda sin problema.
- Nombres con acentos, emojis, RTL o CJK no se prueban en ninguna parte.
- La columna `name` no tiene límite en la base tampoco.

## 5. tests dependientes del reloj

`ReportServiceProjectionsTest` depende de `YearMonth.now()`. Un test que corra a las
23:59 del último día del mes cruza de mes a mitad de ejecución y falla sin que haya
cambiado el código.

Lo que falta: fijar el reloj (o inyectarlo) para que las proyecciones sean
deterministas, y un test del cruce de mes a propósito.

## 6. Lo que se decidió no cubrir

Está en `testing-plan.md` bajo "Qué no vamos a cubrir": carga y rendimiento, E2E de UI,
mails reales, y MCP/Telegram/NLP. Esto último entra en alcance con el agente de Telegram.
