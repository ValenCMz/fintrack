# Plan de Testing

Estrategia de pruebas para el backend de FinTrack. Los 53 endpoints y sus reglas
están documentados en [endpoints.md](endpoints.md).

## Estado actual

No hay cobertura real. Lo único que existe es `FintrackApplicationTests.contextLoads()`,
que verifica que el contexto de Spring arranque y nada más. El backend se probó a mano
con `curl` contra PostgreSQL durante el desarrollo, y eso no se repite solo.

## Qué testeamos y qué no

| Área | Prioridad | Por qué |
|------|-----------|---------|
| Seguridad y ownership | alta | un `403` faltante es una fuga de datos entre usuarios |
| Validación de DTOs | alta | es la primera barrera contra datos basura en la base |
| Proyecciones | alta | es la única lógica de cálculo no trivial |
| Soft delete | alta | el filtro `active` se aplicó en 5 servicios; es regresión fácil |
| Manejo de errores | media | traduce constraints, útil pero no crítico |
| Endpoints CRUD | baja | son CRUD sin lógica; los cubre la integración |
| Frontend | media | sin runner de tests, hoy es verificación manual |

## Stack

- **Unitarios**: JUnit 5 + Mockito + AssertJ. Sin base de datos, sin Spring.
- **Integración**: Testcontainers con PostgreSQL real. No H2.
- **Spring**: `@DataJpaTest` para repositorios, `@SpringBootTest` para el contexto.

PostgreSQL y no H2 por una razón concreta: casi todos los tests que importan son de
constraints (`NOT NULL`, `UNIQUE`, FK) y de filtrado, y H2 no los replica igual. Un test
que pasa en H2 y falla en producción no sirve.

## Dependencias

Testcontainers con versión explícita, en `fintrack/pom.xml`:

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers-junit-jupiter</artifactId>
  <version>2.0.5</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers-postgresql</artifactId>
  <version>2.0.5</version>
  <scope>test</scope>
</dependency>
```

Dos trampas que ya nos mordieron:

1. **No usar `spring-boot-testcontainers`.** Spring Boot 4.0.1 lo gestiona en 2.0.3, y
   localmente solo hay 3.5.6, que no es compatible con Boot 4. Por eso el contenedor se
   levanta a mano con `@DynamicPropertySource` en vez de `@ServiceConnection`.
2. **Fijar la versión.** Sin un BOM de Testcontainers 2.0.5, el `dependencyManagement`
   de Spring lo baja a 2.0.3, cuyas dependencias no están completas en el repo local.
   Declarando `2.0.5` explícito resuelve.

## Configuración de tests

`fintrack/src/test/resources/application-test.properties`. Son 8 variables que
`application.properties` no tiene default, y sin ellas el contexto no arranca:

| Variable | Valor en test |
|----------|---------------|
| `URL_DB` / `USERNAME_DB` / `PASSWORD_DB` | los inyecta Testcontainers |
| `JWT_SECRET` | Base64 válido de 32 bytes o más |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | dummies, no se manda ningún mail |

El JWT secret importa: es una clave Base64, y una inválida hace fallar el contexto por
razones que no tienen nada que ver con el test que estás corriendo.

## Contenedor

Una sola clase base para no repetir configuración:

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestBase {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Requiere Docker. Si no está, los tests de integración se saltan (`@EnabledIf`) y los
unitarios siguen corriendo, que es el comportamiento que queremos en una máquina sin
Docker.

---

## Suite

### Seguridad

| Test | Qué verifica |
|------|--------------|
| `UsuarioServiceUpdateTest` | PATCH: `null` no pisa el campo existente |
| `UsuarioServiceUpdateTest` | un usuario no puede modificar a otro |
| `UsuarioServiceUpdateTest` | un `USER` no puede cambiar su `rol` |
| `UsuarioServiceUpdateTest` | email duplicado se rechaza |
| `UsuarioServiceForgotPasswordTest` | email inexistente responde igual que existente |
| `UsuarioServiceForgotPasswordTest` | email mal formado sí se rechaza |
| `UsuarioServiceResetPasswordTest` | token usado → `400` (bug del token re-servible, cerrado) |
| `UsuarioServiceResetPasswordTest` | token vencido / de otro email revoca y rechaza |
| `JwtServiceKeyTest` | clave Base64 inválida falla al arrancar; válida no |
| `RefreshTokenServiceTest` | refresh no encontrado, vencido o revocado → rechazado |
| `RefreshTokenServiceTest` | refresh reutilizado revoca toda la cadena |
| `SecurityConfigIT` | sin token da `401`; rol insuficiente da `403` |
| `OptimisticLockingTest` | documenta el "lost update" actual (sin `@Version`) |

El test de enumeración de emails es el que más valor tiene de esta tanda: es una
decisión de diseño deliberada, y sin test se pierde en el primer refactor.

### Validación

| Test | Qué verifica |
|------|--------------|
| `MontoValidationTest` | los 8 montos con `@NotNull` rechazan `null` |
| `MontoValidationTest` | `currentAmount` y `remainingAmount` aceptan `0` |
| `MontoValidationTest` | `amount` y `totalAmount` rechazan `0` y negativos |
| `DigitsAmountValidationTest` | `@Digits(36,2)`: 3 decimales y 37 enteros → `400` |
| `DigitsAmountValidationTest` | tope real de la columna (36+2) entra |
| `SizeValidationTest` | `name` de 10.000 chars → `400`; unicode/acentos/emoji entran |

Cubre `TransactionRequest`, `CardRequest`, `DebtRequest`, `SavingGoalRequest`,
`MonotributoRequest` y `FixedExpendeRequest`. El caso de `0` en los dos montos de
progreso es el que se va a romper primero si alguien cambia un `@Positive` por
`@PositiveOrZero` mal puesto.

### Proyecciones

| Test | Qué verifica |
|------|--------------|
| `ReportServiceProjectionsTest` | promedio sobre los últimos 3 meses cerrados |
| `ReportServiceProjectionsTest` | divide siempre por 3, no por meses con datos |
| `ReportServiceProjectionsTest` | excluye el mes en curso |
| `ReportServiceProjectionsTest` | historial vacío devuelve `[]` |
| `ReportServiceProjectionsTest` | `months` fuera de rango da `400` |
| `ReportServiceProjectionsTest` | no filtra por ingresos y gastos juntos |

### Soft delete

Un test por servicio, mismo patrón: crear activo, borrar, verificar que el listado no
lo tiene y que `GET /{id}` sigue devolviéndolo con `active: false`.

- `AccountServiceTest`
- `CategoryServiceTest`
- `CardServiceTest`
- `SavingGoalServiceTest`
- `FixedExpendeServiceTest`

La asimetría es el punto: el listado desaparece, el detalle no. Es intencional para no
romper el historial, y es justo lo que un test fácil no va a marcar.

### Errores

| Test | Qué verifica |
|------|--------------|
| `GlobalExceptionHandlerTest` | `NOT NULL` nullado produce mensaje con el nombre del campo |
| `GlobalExceptionHandlerTest` | `UNIQUE` violado produce mensaje de duplicado |
| `GlobalExceptionHandlerTest` | `DataIntegrityViolationException` desconocida cae a `500` |

### Integración

| Test | Qué verifica |
|------|--------------|
| `AccountRepositoryIT` | `owner` nullable y guardado en la base real |
| `AccountRepositoryIT` | `findByUserIdAndActive` excluye las inactivas y no mezcla usuarios |
| `AccountRepositoryIT` | el filtro `active` de categorías también excluye inactivas |
| `AccountRepositoryIT` | el UNIQUE de email se respeta en la base real |

`owner` nullable merece un test de integración porque se resolvió con un
`ALTER TABLE` manual, no con una migración. El DTO y la entidad lo permiten, pero si
alguien reconstruye la base el `NOT NULL` vuelve y el test lo detecta.

---

## Frontend

Sin runner de tests, y no vamos a agregar uno ahora. La cobertura es manual y está en
[endpoints.md](endpoints.md). Lo que hay que mirar en navegador:

- `/cuentas` y `/categorias`: crear, editar, desactivar, ver la lista.
- Dashboard: que la proyección muestre los meses esperados y `[]` sin historial.
- Que un recurso desactivado en la UI desaparezca de la lista sin refrescar la página.
- Mensajes de error de la API llegando a la pantalla, no tragados.

## Comandos

```bash
# Unitarios: rápido, sin Docker
cd fintrack && mvn -o test

# Unitarios + integración: necesita Docker
cd fintrack && mvn -o verify

# Solo integración
cd fintrack && mvn -o verify -Dit.test='*IT'

# Frontend
cd fintrack-frontend && npx tsc --noEmit && npm run lint && npm run build
```

Los `*IT` corren con **failsafe**, no con surefire: `mvn test` no los toca. Por eso
`mvn -o test` da verde aunque falte Docker, y `mvn -o verify` es el que de verdad
levanta el contenedor.

## Estado actual de la cobertura

| Suite | Tests | Docker |
|-------|-------|--------|
| `ReportServiceProjectionsTest` | 12 | no |
| `MontoValidationTest` | 24 | no |
| `DigitsAmountValidationTest` | 7 | no |
| `SizeValidationTest` | 7 | no |
| `GlobalExceptionHandlerTest` | 12 | no |
| `UsuarioServiceTest` | 19 | no |
| `RefreshTokenServiceTest` | 8 | no |
| `JwtServiceKeyTest` | 10 | no |
| `OptimisticLockingTest` | 3 | no |
| `AccountServiceTest` | 8 | no |
| `CategoryServiceSoftDeleteTest` | 4 | no |
| `SoftDeleteServicesTest` | 13 | no |
| `AccountRepositoryIT` | 6 | **sí** |
| `SecurityConfigIT` | 6 | no |
| `FintrackApplicationTests` | 1 | no (usa la base de desarrollo) |

127 unitarios y 6 de integración, verdes.

## Dos trampas que aparecieron al implementarlo

1. **`DataJpaTest` cambió de paquete en Boot 4.** Ahora es
   `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, no
   `org.springframework.boot.test.autoconfigure.orm.jpa`. Con el import viejo no compila.
2. **`@Container` no levanta a tiempo con `@DataJpaTest`.** El contexto de Spring se
   crea antes de que la extensión de Testcontainers arranque el contenedor, y la
   conexión falla. Hay que arrancarlo en un bloque `static` y registrar la URL con
   `@DynamicPropertySource`.


## Orden de ejecución

1. Unitarios: instantánea, corren en cada guardado.
2. Integración: antes de commitear.
3. `mvn -o test` completo: antes de abrir PR.
4. Frontend: `tsc`, `lint`, `build`.
5. Paseo manual en navegador: antes de releases.

## Qué no vamos a cubrir

- Carga y rendimiento. No hay volumen que lo justifique todavía.
- Tests end-to-end de la UI. Sin runner, y el backend ya cubre el contrato.
- Integraciones de mail reales. Con dummies alcanza para el contrato.
- `MCP`, `Telegram` y NLP. Fuera de alcance de esta fase.
