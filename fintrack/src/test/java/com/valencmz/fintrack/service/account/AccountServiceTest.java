package com.valencmz.fintrack.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.AccountType;
import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.dto.account.AccountRequest;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;

@DisplayName("Cuentas: baja logica, ownership y titular opcional")
class AccountServiceTest {

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

        when(accountRepository.findByUserIdAndActive(any(), anyBoolean())).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Account account(String name, boolean active) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setName(name);
        a.setType(AccountType.BANK);
        a.setActive(active);
        a.setUser(userAuth.getUser());
        return a;
    }

    @Nested
    @DisplayName("baja logica")
    class BajaLogica {

        @Test
        @DisplayName("desactiva la cuenta en vez de borrarla")
        void desactivaEnVezDeBorrar() {
            Account existing = account("Santander", true);
            when(accountRepository.findByIdAndUserId(existing.getId(), userId))
                    .thenReturn(Optional.of(existing));

            accountService.softDelete(existing.getId(), userAuth);

            assertThat(existing.isActive()).isFalse();
            verify(accountRepository).save(existing);
            verify(accountRepository, never()).delete(any());
        }

        @Test
        @DisplayName("el listado consulta solo las activas")
        void elListadoConsultaSoloLasActivas() {
            when(accountRepository.findByUserIdAndActive(userId, true))
                    .thenReturn(List.of(account("Santander", true)));

            accountService.getByUser(userAuth);

            verify(accountRepository).findByUserIdAndActive(userId, true);
            verify(accountRepository, never()).findByUserIdAndActive(userId, false);
        }

        @Test
        @DisplayName("el detalle sigue devolviendo la cuenta desactivada")
        void elDetalleSigueDevolviendoLaDesactivada() {
            Account deactivated = account("Santander", false);
            when(accountRepository.findByIdAndUserId(deactivated.getId(), userId))
                    .thenReturn(Optional.of(deactivated));

            assertThat(accountService.getById(deactivated.getId(), userAuth).isActive()).isFalse();
        }

        @Test
        @DisplayName("borrar una cuenta ajena da 404")
        void borrarCuentaAjenaDa404() {
            UUID id = UUID.randomUUID();
            when(accountRepository.findByIdAndUserId(eq(id), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.softDelete(id, userAuth))
                    .isInstanceOf(CustomAppException.class)
                    .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("titular opcional")
    class TitularOpcional {

        @Test
        @DisplayName("crea una cuenta sin titular")
        void creaSinTitular() {
            AccountRequest request = new AccountRequest();
            request.setName("Dolar");
            request.setType(AccountType.WALLET);
            request.setOwner(null);
            request.setActive(true);

            accountService.create(request, userAuth);

            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("actualizar con titular null lo deja en null, no lo pisa con vacio")
        void actualizarConTitularNullLoDejaEnNull() {
            Account existing = account("Santander", true);
            existing.setOwner("Valentin");
            when(accountRepository.findByIdAndUserId(existing.getId(), userId))
                    .thenReturn(Optional.of(existing));

            AccountRequest request = new AccountRequest();
            request.setName("Santander");
            request.setType(AccountType.BANK);
            request.setOwner(null);
            request.setActive(true);

            accountService.update(existing.getId(), request, userAuth);

            assertThat(existing.getOwner()).isNull();
        }

        @Test
        @DisplayName("actualizar cambia el titular cuando viene uno")
        void actualizarCambiaElTitular() {
            Account existing = account("Santander", true);
            when(accountRepository.findByIdAndUserId(existing.getId(), userId))
                    .thenReturn(Optional.of(existing));

            AccountRequest request = new AccountRequest();
            request.setName("Santander");
            request.setType(AccountType.BANK);
            request.setOwner("Otro");
            request.setActive(true);

            accountService.update(existing.getId(), request, userAuth);

            assertThat(existing.getOwner()).isEqualTo("Otro");
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("un PUT con active true reactiva la cuenta")
        void unPutReactivaLaCuenta() {
            Account existing = account("Santander", false);
            when(accountRepository.findByIdAndUserId(existing.getId(), userId))
                    .thenReturn(Optional.of(existing));

            AccountRequest request = new AccountRequest();
            request.setName("Santander");
            request.setType(AccountType.BANK);
            request.setActive(true);

            accountService.update(existing.getId(), request, userAuth);

            assertThat(existing.isActive()).isTrue();
        }
    }
}
