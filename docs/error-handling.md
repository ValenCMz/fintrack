# Sistema de Manejo de Errores

## Visión General

FinTrack utiliza un patrón combinado de **Result + Global Exception Handler** para manejar errores de forma consistente en toda la API.

```
Request → Controller → Service → Result<T> → Controller → ApiResponse
                  ↓ (excepción)
         GlobalExceptionHandler → ErrorResponse
```

## Estructura de Respuestas

### Respuesta Exitosa

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@test.com",
    "username": "juan"
  }
}
```

### Respuesta de Error

```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "Usuario no encontrado",
    "status": 404,
    "statusText": "Not Found"
  }
}
```

## Componentes

### 1. `ApiResponse<T>` — Envelope Genérico

Clase base para todas las respuestas HTTP.

**Paquete:** `com.valencmz.fintrack.errors`

```java
@Getter @Setter
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```

### 2. `ErrorResponse` — Respuesta de Error

Extiende `ApiResponse` con un objeto `ErrorDetail` que contiene el código de error, mensaje, status HTTP y su descripción.

```java
public class ErrorResponse extends ApiResponse<Object> {
    private ErrorDetail error;

    @Getter @AllArgsConstructor
    public static class ErrorDetail {
        private String code;      // Ej: "USER_NOT_FOUND"
        private String message;   // Ej: "Usuario no encontrado"
        private int status;       // Ej: 404
        private String statusText;// Ej: "Not Found"
    }
}
```

### 3. `Result<T>` — Pattern Result para Services

Envuelve el resultado de una operación de service. Puede ser éxito (con datos) o fallo (con error).

**Paquete:** `com.valencmz.fintrack.errors`

```java
Result<UserDTO> result = usuarioService.getUser(id);
if (result.isSuccess()) {
    return ResponseEntity.ok(ApiResponse.success(result.getData()));
}
return ResponseEntity.status(result.getErrorCode().getStatus())
    .body(ApiResponse.error(result.getErrorCode(), result.getMessage()));
```

**Métodos estáticos:**

| Método | Descripción |
|--------|-------------|
| `Result.success(data)` | Crear resultado exitoso con datos |
| `Result.failure(errorCode)` | Crear resultado fallido con código de error (usa mensaje default) |
| `Result.failure(errorCode, message)` | Crear resultado fallido con mensaje personalizado |

### 4. `ErrorCode` — Enum de Códigos de Error

Define los códigos de error de la aplicación con su mensaje por defecto y status HTTP asociado.

**Paquete:** `com.valencmz.fintrack.enums`

| Código | Mensaje Default | Status |
|--------|----------------|--------|
| `INVALID_CREDENTIALS` | Credenciales inválidas | 401 |
| `EMAIL_ALREADY_EXISTS` | Ya existe un usuario con ese email | 400 |
| `UNAUTHORIZED` | No autenticado | 401 |
| `FORBIDDEN` | No tenés permisos | 403 |
| `TOKEN_EXPIRED` | Token expirado | 401 |
| `REFRESH_TOKEN_INVALID` | Refresh token inválido | 401 |
| `USER_NOT_FOUND` | Usuario no encontrado | 404 |
| `VALIDATION_ERROR` | Error de validación | 400 |
| `INVALID_EMAIL_FORMAT` | Formato de email inválido | 400 |
| `PASSWORD_TOO_SHORT` | La contraseña debe tener al menos 8 caracteres | 400 |
| `NOT_FOUND` | Recurso no encontrado | 404 |
| `INTERNAL_ERROR` | Error interno del servidor | 500 |

### 5. `CustomAppException` — Excepción de Negocio

Excepción personalizada que lleva `HttpStatus` + `ErrorCode`. Se lanza desde los services y es capturada por el `GlobalExceptionHandler`.

```java
throw new CustomAppException(
    "Ya existe un usuario con ese email",
    HttpStatus.BAD_REQUEST,
    ErrorCode.EMAIL_ALREADY_EXISTS
);
```

### 6. `GlobalExceptionHandler` — Manejador Centralizado

Captura excepciones y las convierte en `ErrorResponse` con el HTTP status correcto.

**Paquete:** `com.valencmz.fintrack.config`

| Excepción Manejada | ErrorCode | Status |
|--------------------|-----------|--------|
| `CustomAppException` | (el de la excepción) | (el de la excepción) |
| `BadCredentialsException` | `INVALID_CREDENTIALS` | 401 |
| `UsernameNotFoundException` | `USER_NOT_FOUND` | 404 |
| `MethodArgumentNotValidException` | `VALIDATION_ERROR` | 400 |
| `Exception` (fallback) | `INTERNAL_ERROR` | 500 |

## Cómo Usar

### En un Service

**Flujo normal (éxito):**
```java
public Result<UserDTO> getUser(UUID id) {
    User user = userRepository.findById(id).orElse(null);
    if (user == null) {
        return Result.failure(ErrorCode.USER_NOT_FOUND);
    }
    return Result.success(new UserDTO(user));
}
```

**Flujo de error (lanzar excepción):**
```java
public void register(RegisterDTO dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new CustomAppException(
            "Ya existe un usuario con ese email",
            HttpStatus.BAD_REQUEST,
            ErrorCode.EMAIL_ALREADY_EXISTS
        );
    }
    // ... guardar usuario
}
```

### En un Controller

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserDTO>> me(@AuthenticationPrincipal UserAuth user) {
    Result<UserDTO> result = usuarioService.getUserInfo(user);
    if (result.isSuccess()) {
        return ResponseEntity.ok(ApiResponse.success(result.getData()));
    }
    ErrorCode code = result.getErrorCode();
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.error(code, result.getMessage()));
}
```

### En un Controller con Validación

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<RegisterReponseDTO>> register(
        @Valid @RequestBody RegisterDTO registerDTO) {
    // @Valid activa la validación de Jakarta
    // Si falla, GlobalExceptionHandler captura MethodArgumentNotValidException
    RegisterReponseDTO response = usuarioService.register(registerDTO);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

## Agregar Nuevos Códigos de Error

1. Agregar el código al enum `ErrorCode`:

```java
// En enums/ErrorCode.java
ACCOUNT_NOT_FOUND("Cuenta no encontrada", HttpStatus.NOT_FOUND),
CARD_EXPIRED("Tarjeta vencida", HttpStatus.BAD_REQUEST),
```

2. Usarlo en un service:

```java
return Result.failure(ErrorCode.ACCOUNT_NOT_FOUND);
// o con mensaje custom:
return Result.failure(ErrorCode.CARD_EXPIRED, "La tarjeta Visa vence mañana");
```

3. O lanzar como excepción:

```java
throw new CustomAppException(
    "Cuenta no encontrada",
    HttpStatus.NOT_FOUND,
    ErrorCode.ACCOUNT_NOT_FOUND
);
```

## Flujo Completo

```
1. Client envía request a /api/users/invalid-id
2. UserController llama a usuarioService.getUser(id)
3. Service busca en BD → no encuentra usuario
4. Service retorna Result.failure(ErrorCode.USER_NOT_FOUND)
5. Controller retorna ResponseEntity con ErrorResponse
6. Respuesta HTTP 404:
   {
     "success": false,
     "error": {
       "code": "USER_NOT_FOUND",
       "message": "Usuario no encontrado",
       "status": 404,
       "statusText": "Not Found"
     }
   }
```
