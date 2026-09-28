# Sistema de Manejo de Errores

## Principio

Un solo mecanismo: **"lanzá, no devuelvas errores."**

```
Request → Controller (delgado, sin try/catch) → Service (lanza CustomAppException o @Valid falla)
                                                        ↓ (excepción)
                                         GlobalExceptionHandler → ApiResponse
```

- Los **services lanzan** `CustomAppException` (o dejan que `@Valid` falle).
- Los **controllers no manejan errores**: solo devuelven el dato envuelto en `ApiResponse.success(...)`.
- El **`GlobalExceptionHandler`** convierte toda excepción en un `ApiResponse` de error.

## Envelope único — `ApiResponse<T>`

Un único formato para éxito y error (los campos `null` se omiten).

### Respuesta exitosa

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

### Respuesta de error

```json
{
  "success": false,
  "error": {
    "status": 404,
    "message": "Cuenta no encontrada"
  }
}
```

### API de `ApiResponse`

```java
ApiResponse.success(data);              // 200
ApiResponse.error(HttpStatus.NOT_FOUND, "Cuenta no encontrada"); // error
```

## Componentes

| Componente | Rol |
|-----------|-----|
| `ApiResponse<T>` | Envelope único. Factories `success(data)` / `error(status, message)` |
| `CustomAppException` | Excepción de negocio con `HttpStatus`. Se lanza desde services |
| `GlobalExceptionHandler` | `@RestControllerAdvice` que atrapa excepciones y devuelve `ApiResponse` |

## Excepciones manejadas

| Excepción | Status | Mensaje |
|-----------|--------|---------|
| `CustomAppException` | el de la excepción | el de la excepción |
| `BadCredentialsException` | 401 | Credenciales inválidas |
| `UsernameNotFoundException` | 404 | Usuario no encontrado |
| `MethodArgumentNotValidException` | 400 | `campo: mensaje` de validación |
| `HttpMessageNotReadableException` | 400 | Cuerpo de la petición inválido |
| `AccessDeniedException` | 403 | No tiene permisos para realizar esta acción |
| `DataIntegrityViolationException` | 409 | Conflicto con los datos existentes |
| `Exception` (fallback) | 500 | Error interno del servidor |

> Los errores de autenticación JWT se manejan en `JwtFilter` (401) porque corren **antes** del `DispatcherServlet`.

## Cómo usar

### En un Service

```java
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    // Éxito: devuelve data directa (el controller envuelve en ApiResponse)
    public List<AccountResponse> getByUser(UserAuth userAuth) {
        return accountRepository.findByUserIdAndActive(userAuth.getUser().getId(), true)
                .stream().map(AccountResponse::new).toList();
    }

    // Error: lanza excepción
    public AccountResponse getById(UUID id, UserAuth userAuth) {
        Account account = findOwnedAccount(id, userAuth.getUser());
        return new AccountResponse(account);
    }

    private Account findOwnedAccount(UUID id, User user) {
        return accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new CustomAppException("Cuenta no encontrada", HttpStatus.NOT_FOUND));
    }
}
```

### En un Controller

```java
@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAll(
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getByUser(userAuth)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserAuth userAuth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountService.create(request, userAuth)));
    }
}
```

## Flujo completo

```
1. Client: GET /accounts/9999 (id inexistente)
2. AccountController llama a accountService.getById(id, user)
3. Service busca en BD → no encuentra → lanza CustomAppException("Cuenta no encontrada", 404)
4. La excepción sube hasta GlobalExceptionHandler (nadie la atrapa en el medio)
5. GlobalExceptionHandler devuelve:
   HTTP 404
   { "success": false, "error": { "status": 404, "message": "Cuenta no encontrada" } }
```
