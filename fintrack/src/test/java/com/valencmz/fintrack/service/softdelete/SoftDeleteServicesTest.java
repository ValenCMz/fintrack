package com.valencmz.fintrack.service.softdelete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.Rol;
import com.valencmz.fintrack.errors.CustomAppException;
import com.valencmz.fintrack.model.entity.Card;
import com.valencmz.fintrack.model.entity.FixedExpende;
import com.valencmz.fintrack.model.entity.SavingGoal;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.CardInstallmentRepository;
import com.valencmz.fintrack.repository.CardPaymentRepository;
import com.valencmz.fintrack.repository.CardRepository;
import com.valencmz.fintrack.repository.FixedExpendeRepository;
import com.valencmz.fintrack.repository.SavingGoalRepository;
import com.valencmz.fintrack.repository.TransactionRepository;
import com.valencmz.fintrack.service.card.CardService;
import com.valencmz.fintrack.service.fixedexpense.FixedExpendeService;
import com.valencmz.fintrack.service.savinggoal.SavingGoalService;

/**
 * Los tres servicios comparten el mismo patron de baja logica. Un test por recurso
 * para que un servicio que se desvíe del patron quede en rojo explicitamente, no
 * porque el otro todavia lo cumpla.
 */
@DisplayName("Tarjetas, metas de ahorro y gastos fijos: baja lógica")
class SoftDeleteServicesTest {

    private UserAuth userAuth;
    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRol(Rol.USER);
        userId = user.getId();
        userAuth = new UserAuth(user);
    }

    private Card card(boolean active) {
        Card c = new Card();
        c.setId(UUID.randomUUID());
        c.setHolderName("Valentin");
        c.setDueDayOfMonth(10);
        c.setActive(active);
        c.setUser(userAuth.getUser());
        return c;
    }

    private SavingGoal goal(boolean active) {
        SavingGoal g = new SavingGoal();
        g.setId(UUID.randomUUID());
        g.setName("Fondo");
        g.setTargetAmount(new java.math.BigDecimal("300000"));
        g.setCurrentAmount(java.math.BigDecimal.ZERO);
        g.setTargetDate(LocalDate.now().plusDays(100));
        g.setActive(active);
        g.setUser(userAuth.getUser());
        return g;
    }

    private FixedExpende fixed(boolean active) {
        FixedExpende f = new FixedExpende();
        f.setId(UUID.randomUUID());
        f.setName("Alquiler");
        f.setAmount(new java.math.BigDecimal("80000"));
        f.setDueDay(LocalDate.now().plusDays(5));
        f.setFrequency("mensual");
        f.setActive(active);
        f.setUser(userAuth.getUser());
        return f;
    }

    // ---------- tarjetas ----------

    @Test
    @DisplayName("tarjeta: desactiva en vez de borrar")
    void tarjetaDesactivaEnVezDeBorrar() {
        CardRepository repo = mock(CardRepository.class);
        CardService service = new CardService();
        ReflectionTestUtils.setField(service, "cardRepository", repo);

        Card existing = card(true);
        when(repo.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(repo.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));

        service.softDelete(existing.getId(), userAuth);

        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
        verify(repo, never()).delete(any());
    }

    @Test
    @DisplayName("tarjeta: el listado consulta solo las activas")
    void tarjetaListadoSoloActivas() {
        CardRepository repo = mock(CardRepository.class);
        CardService service = new CardService();
        ReflectionTestUtils.setField(service, "cardRepository", repo);
        when(repo.findByUserIdAndActive(any(), anyBoolean())).thenReturn(List.of());

        service.getByUser(userAuth);

        verify(repo).findByUserIdAndActive(userId, true);
        verify(repo, never()).findByUserIdAndActive(userId, false);
    }

    @Test
    @DisplayName("tarjeta: el detalle sigue devolviendo la desactivada")
    void tarjetaDetalleDevuelveDesactivada() {
        CardRepository repo = mock(CardRepository.class);
        CardService service = new CardService();
        ReflectionTestUtils.setField(service, "cardRepository", repo);
        TransactionRepository transactionRepo = mock(TransactionRepository.class);
        CardPaymentRepository paymentRepo = mock(CardPaymentRepository.class);
        CardInstallmentRepository installmentRepo = mock(CardInstallmentRepository.class);
        when(transactionRepo.findByCardIdAndUserId(any(), any())).thenReturn(List.of());
        when(paymentRepo.findByCardIdAndUserId(any(), any())).thenReturn(List.of());
        when(installmentRepo.findByCardIdAndPaidOrderByDueDateAsc(any(), anyBoolean())).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "transactionRepository", transactionRepo);
        ReflectionTestUtils.setField(service, "cardPaymentRepository", paymentRepo);
        ReflectionTestUtils.setField(service, "cardInstallmentRepository", installmentRepo);

        Card deactivated = card(false);
        when(repo.findByIdAndUserId(deactivated.getId(), userId)).thenReturn(Optional.of(deactivated));

        assertThat(service.getById(deactivated.getId(), userAuth).isActive()).isFalse();
    }

    @Test
    @DisplayName("tarjeta: borrar una ajena da 404")
    void tarjetaAjenaDa404() {
        CardRepository repo = mock(CardRepository.class);
        CardService service = new CardService();
        ReflectionTestUtils.setField(service, "cardRepository", repo);
        when(repo.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(UUID.randomUUID(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(repo, never()).save(any());
    }

    // ---------- metas de ahorro ----------

    @Test
    @DisplayName("meta: desactiva en vez de borrar")
    void metaDesactivaEnVezDeBorrar() {
        SavingGoalRepository repo = mock(SavingGoalRepository.class);
        SavingGoalService service = new SavingGoalService();
        ReflectionTestUtils.setField(service, "savingGoalRepository", repo);

        SavingGoal existing = goal(true);
        when(repo.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(repo.save(any(SavingGoal.class))).thenAnswer(i -> i.getArgument(0));

        service.softDelete(existing.getId(), userAuth);

        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
        verify(repo, never()).delete(any());
    }

    @Test
    @DisplayName("meta: el listado consulta solo las activas")
    void metaListadoSoloActivas() {
        SavingGoalRepository repo = mock(SavingGoalRepository.class);
        SavingGoalService service = new SavingGoalService();
        ReflectionTestUtils.setField(service, "savingGoalRepository", repo);
        when(repo.findByUserIdAndActive(any(), anyBoolean())).thenReturn(List.of());

        service.getByUser(userAuth);

        verify(repo).findByUserIdAndActive(userId, true);
        verify(repo, never()).findByUserIdAndActive(userId, false);
    }

    @Test
    @DisplayName("meta: el detalle sigue devolviendo la desactivada")
    void metaDetalleDevuelveDesactivada() {
        SavingGoalRepository repo = mock(SavingGoalRepository.class);
        SavingGoalService service = new SavingGoalService();
        ReflectionTestUtils.setField(service, "savingGoalRepository", repo);

        SavingGoal deactivated = goal(false);
        when(repo.findByIdAndUserId(deactivated.getId(), userId)).thenReturn(Optional.of(deactivated));

        assertThat(service.getById(deactivated.getId(), userAuth).isActive()).isFalse();
    }

    @Test
    @DisplayName("meta: borrar una ajena da 404")
    void metaAjenaDa404() {
        SavingGoalRepository repo = mock(SavingGoalRepository.class);
        SavingGoalService service = new SavingGoalService();
        ReflectionTestUtils.setField(service, "savingGoalRepository", repo);
        when(repo.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(UUID.randomUUID(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(repo, never()).save(any());
    }

    // ---------- gastos fijos ----------

    @Test
    @DisplayName("gasto fijo: desactiva en vez de borrar")
    void gastoFijoDesactivaEnVezDeBorrar() {
        FixedExpendeRepository repo = mock(FixedExpendeRepository.class);
        FixedExpendeService service = fixedExpendeService(repo);

        FixedExpende existing = fixed(true);
        when(repo.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(repo.save(any(FixedExpende.class))).thenAnswer(i -> i.getArgument(0));

        service.softDelete(existing.getId(), userAuth);

        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
        verify(repo, never()).delete(any());
    }

    @Test
    @DisplayName("gasto fijo: el listado consulta solo las activas")
    void gastoFijoListadoSoloActivas() {
        FixedExpendeRepository repo = mock(FixedExpendeRepository.class);
        FixedExpendeService service = fixedExpendeService(repo);
        when(repo.findByUserIdAndActive(any(), anyBoolean())).thenReturn(List.of());

        service.getByUser(userAuth);

        verify(repo).findByUserIdAndActive(userId, true);
        verify(repo, never()).findByUserIdAndActive(userId, false);
    }

    @Test
    @DisplayName("gasto fijo: el detalle sigue devolviendo el desactivado")
    void gastoFijoDetalleDevuelveDesactivado() {
        FixedExpendeRepository repo = mock(FixedExpendeRepository.class);
        FixedExpendeService service = fixedExpendeService(repo);

        FixedExpende deactivated = fixed(false);
        when(repo.findByIdAndUserId(deactivated.getId(), userId)).thenReturn(Optional.of(deactivated));

        assertThat(service.getById(deactivated.getId(), userAuth).isActive()).isFalse();
    }

    @Test
    @DisplayName("gasto fijo: borrar uno ajeno da 404")
    void gastoFijoAjenoDa404() {
        FixedExpendeRepository repo = mock(FixedExpendeRepository.class);
        FixedExpendeService service = fixedExpendeService(repo);
        when(repo.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(UUID.randomUUID(), userAuth))
                .isInstanceOf(CustomAppException.class)
                .satisfies(e -> assertThat(((CustomAppException) e).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("gasto fijo: la frecuencia se guarda tal cual, sin normalizar")
    void laFrecuenciaNoSeNormaliza() {
        FixedExpendeRepository repo = mock(FixedExpendeRepository.class);
        FixedExpendeService service = fixedExpendeService(repo);

        FixedExpende existing = fixed(true);
        existing.setFrequency("cada 15 dias cuando tenga ganas");
        when(repo.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(repo.save(any(FixedExpende.class))).thenAnswer(i -> i.getArgument(0));

        service.softDelete(existing.getId(), userAuth);

        assertThat(existing.getFrequency()).isEqualTo("cada 15 dias cuando tenga ganas");
    }

    private FixedExpendeService fixedExpendeService(FixedExpendeRepository repo) {
        FixedExpendeService service = new FixedExpendeService();
        ReflectionTestUtils.setField(service, "fixedExpendeRepository", repo);
        ReflectionTestUtils.setField(service, "categoryRepository",
                mock(com.valencmz.fintrack.repository.CategoryRepository.class));
        ReflectionTestUtils.setField(service, "accountRepository",
                mock(com.valencmz.fintrack.repository.AccountRepository.class));
        return service;
    }
}
