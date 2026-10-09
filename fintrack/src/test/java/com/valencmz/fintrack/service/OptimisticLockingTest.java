package com.valencmz.fintrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.AccountType;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.model.dto.account.AccountRequest;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.service.account.AccountService;

/**
 * Documenta la decisión conocida: las entidades no tienen @Version, así que
 * dos PUT simultáneos sobre la misma cuenta se pisan en silencio y gana el
 * último. No es un bug que se arregle con un test; es un comportamiento que
 * conviene dejar anclado para que, el día que se agregue locking optimista,
 * estos tests cambien y quede claro que el comportamiento anterior se fue.
 */
@DisplayName("Concurrencia: sin @Version el update pierde silenciosamente")
class OptimisticLockingTest {

    private static final List<String> ENTIDADES = List.of(
            "com.valencmz.fintrack.model.entity.Account",
            "com.valencmz.fintrack.model.entity.Card",
            "com.valencmz.fintrack.model.entity.Category",
            "com.valencmz.fintrack.model.entity.Debt",
            "com.valencmz.fintrack.model.entity.FixedExpende",
            "com.valencmz.fintrack.model.entity.Monotributo",
            "com.valencmz.fintrack.model.entity.RefreshToken",
            "com.valencmz.fintrack.model.entity.SavingGoal",
            "com.valencmz.fintrack.model.entity.Transaction",
            "com.valencmz.fintrack.model.entity.User",
            "com.valencmz.fintrack.model.entity.auth.PasswordResetToken");

    private AccountRepository accountRepository;
    private AccountService accountService;
    private UserAuth userAuth;
    private UUID userId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", accountRepository);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRol(Rol.USER);
        userId = user.getId();
        userAuth = new UserAuth(user);
    }

    @Test
    @DisplayName("la lista de entidades del test no se quedó vieja")
    void sonOnceEntidades() {
        assertThat(ENTIDADES).hasSize(11);
    }

    @Test
    @DisplayName("ninguna entidad declara @Version (hoy no hay locking optimista)")
    void ningunaEntidadDeclaraVersion() {
        List<String> conVersion = ENTIDADES.stream()
                .filter(clazz -> {
                    try {
                        Class<?> type = Class.forName(clazz);
                        return java.util.Arrays.stream(type.getDeclaredFields())
                                .anyMatch(f -> f.isAnnotationPresent(jakarta.persistence.Version.class));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();

        // Si alguna entidad aparece aca, alguien agregó @Version y este test
        // documenta la ausencia. La intención es que se actualice para verificar
        // el locking, no que se mantenga verde sin mirarlo.
        assertThat(conVersion).isEmpty();
    }

    @Test
    @DisplayName("dos PUT consecutivos sobre la misma cuenta: gana el último, sin conflicto")
    void doblePutGanaElUltimo() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setName("Santander");
        account.setType(AccountType.BANK);
        account.setActive(true);
        account.setUser(userAuth.getUser());
        when(accountRepository.findByIdAndUserId(eq(account.getId()), eq(userId)))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        accountService.update(account.getId(), request("Edicion A"), userAuth);
        accountService.update(account.getId(), request("Edicion B"), userAuth);

        // el segundo PUT pisa al primero sin avisar: no hay OptimisticLockException
        assertThat(account.getName()).isEqualTo("Edicion B");
        verify(accountRepository, times(2)).save(account);
    }

    private AccountRequest request(String name) {
        AccountRequest r = new AccountRequest();
        r.setName(name);
        r.setType(AccountType.BANK);
        r.setActive(true);
        return r;
    }
}