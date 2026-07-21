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
    "status": 404,
    "statusText": "Not Found",
    "message": "Usuario no encontrado"
  }
}
```

El frontend puede decidir cómo actuar según el `status` HTTP (401 = login, 403 = sin permisos, 404 = no encontrado, etc.) y mostrar el `message` al usuario.

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

Extiende `ApiResponse` con un objeto `ErrorDetail` que contiene el status HTTP y su descripción.

```java
public class ErrorResponse extends ApiResponse<Object> {
    private ErrorDetail error;

    public ErrorResponse(String message, HttpStatus status) {
        super(false, null, null);
        this.error = new ErrorDetail(status.value(), status.getReasonPhrase(), message);
    }

    @Getter @AllArgsConstructor
    public static class ErrorDetail {
        private int status;       // Ej: 404
        private String statusText;// Ej: "Not Found"
        private String message;   // Ej: "Usuario no encontrado"
    }
}
```

### 3. `Result<T>` — Pattern Result para Services

Envuelve el resultado de una operación de service. Puede ser éxito (con datos) o fallo (con mensaje).

**Paquete:** `com.valencmz.fintrack.errors`

```java
@Getter
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> failure(String message) { ... }
}
```

### 4. `CustomAppException` — Excepción de Negocio

Excepción personalizada que lleva `HttpStatus`. Se lanza desde los services y es capturada por el `GlobalExceptionHandler`.

**Paquete:** `com.valencmz.fintrack.errors`

```java
public class CustomAppException extends RuntimeException {
    private final HttpStatus httpStatus;

    public CustomAppException(String message, HttpStatus httpStatus) { ... }
    public HttpStatus getHttpStatus() { ... }
}
```

### 5. `GlobalExceptionHandler` — Manejador Centralizado

Captura excepciones y las convierte en `ErrorResponse` con el HTTP status correcto.

**Paquete:** `com.valencmz.fintrack.config`

| Excepción Manejada | Status | Mensaje |
|--------------------|--------|---------|
| `CustomAppException` | (el de la excepción) | (mensaje de la excepción) |
| `BadCredentialsException` | 401 Unauthorized | Credenciales inválidas |
| `UsernameNotFoundException` | 404 Not Found | Usuario no encontrado |
| `MethodArgumentNotValidException` | 400 Bad Request | Campo: mensaje de validación |
| `Exception` (fallback) | 500 Internal Server Error | Mensaje de la excepción |

## Cómo Usar

### En un Service

**Flujo normal (éxito):**
```java
public Result<UserDTO> getUser(UUID id) {
    User user = userRepository.findById(id).orElse(null);
    if (user == null) {
        return Result.failure("Usuario no encontrado");
    }
    return Result.success(new UserDTO(user));
}
```

**Flujo de error (lanzar excepción):**
```java
public void register(RegisterDTO dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new CustomAppException("Ya existe un usuario con ese email", HttpStatus.BAD_REQUEST);
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
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(result.getErrorMessage()));
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

## Flujo Completo

```
1. Client envía request GET /accounts/invalid-id
2. AccountController llama a accountService.getAccount(id, user)
3. Service busca en BD → no encuentra la cuenta
4. Service retorna Result.failure("Cuenta no encontrada")
5. Controller retorna ResponseEntity con ErrorResponse
6. Respuesta HTTP 404:
   {
     "success": false,
     "error": {
       "status": 404,
       "statusText": "Not Found",
       "message": "Cuenta no encontrada"
     }
   }
```
