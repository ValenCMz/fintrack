package com.valencmz.fintrack.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.model.dto.report.ProjectionResponse;
import com.valencmz.fintrack.model.entity.Account;
import com.valencmz.fintrack.model.entity.Category;
import com.valencmz.fintrack.model.entity.Transaction;
import com.valencmz.fintrack.model.entity.User;
import com.valencmz.fintrack.model.entity.auth.UserAuth;
import com.valencmz.fintrack.repository.AccountRepository;
import com.valencmz.fintrack.repository.TransactionRepository;

/**
 * El reloj esta fijado a AHORA = junio 2026. Antes estas proyecciones dependian
 * de YearMonth.now() y un test que corriera a las 23:59 del ultimo dia del mes
 * cruzaba de mes a mitad de ejecucion y fallaba sin que hubiera cambiado el
 * codigo. Con el reloj inyectado son deterministicas, y los tests de cruce de
 * mes lo prueban a proposito.
 */
@DisplayName("GET /api/reports/projections")
class ReportServiceProjectionsTest {

    private static final int HISTORY_MONTHS = 3;
    private static final YearMonth AHORA = YearMonth.of(2026, 6);

    private TransactionRepository transactionRepository;
    private ReportService reportService;
    private UserAuth userAuth;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of());

        reportService = new ReportService();
        reportService.setClock(Clock.fixed(
                AHORA.atDay(15).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        ReflectionTestUtils.setField(reportService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(reportService, "accountRepository", mock(AccountRepository.class));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRol(com.valencmz.fintrack.enums.Rol.USER);
        userAuth = new UserAuth(user);
    }

    private void history(Transaction... transactions) {
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(transactions));
    }

    private Transaction transaction(TransactionType type, String amount, YearMonth month) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setName("Santander");
        account.setUser(userAuth.getUser());

        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Alquiler");
        category.setUser(userAuth.getUser());

        return new Transaction(
                UUID.randomUUID(),
                type,
                "movimiento",
                new BigDecimal(amount),
                month.atDay(10),
                null,
                category,
                account,
                userAuth.getUser());
    }

    @Nested
    @DisplayName("calculo del promedio")
    class CalculoDelPromedio {

        @Test
        @DisplayName("promedia los ultimos 3 meses cerrados")
        void promediaLosUltimosTresMesesCerrados() {
            YearMonth last = AHORA.minusMonths(1);
            history(
                    transaction(TransactionType.INCOME, "300000", last.minusMonths(2)),
                    transaction(TransactionType.INCOME, "300000", last.minusMonths(1)),
                    transaction(TransactionType.INCOME, "300000", last),
                    transaction(TransactionType.EXPENSE, "100000", last.minusMonths(2)),
                    transaction(TransactionType.EXPENSE, "100000", last.minusMonths(1)),
                    transaction(TransactionType.EXPENSE, "100000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEstimatedIncome()).isEqualByComparingTo("300000.00");
            assertThat(result.get(0).getEstimatedExpense()).isEqualByComparingTo("100000.00");
            assertThat(result.get(0).getEstimatedNet()).isEqualByComparingTo("200000.00");
        }

        @Test
        @DisplayName("divide siempre por 3, no por la cantidad de meses con datos")
        void divideSiemprePorTres() {
            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result.get(0).getEstimatedExpense()).isEqualByComparingTo("100000.00");
        }

        @Test
        @DisplayName("el filtro de fechas abarca los 3 meses cerrados completos")
        void elFiltroAbarcaTresMesesCerrados() {
            YearMonth firstMonth = AHORA.minusMonths(HISTORY_MONTHS);

            history(transaction(TransactionType.INCOME, "300000", AHORA.minusMonths(1)));
            reportService.projections(userAuth, 3);

            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(firstMonth.atDay(1)),
                    eq(AHORA.atDay(1).minusDays(1)));
        }

        @Test
        @DisplayName("el historico excluye el mes en curso por la ventana de fechas")
        void elHistoricoExcluyeElMesEnCurso() {
            history(transaction(TransactionType.INCOME, "300000", AHORA.minusMonths(1)));

            reportService.projections(userAuth, 1);

            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(AHORA.minusMonths(HISTORY_MONTHS).atDay(1)),
                    eq(AHORA.atDay(1).minusDays(1)));
        }

        @Test
        @DisplayName("separa ingresos de gastos")
        void separaIngresosDeGastos() {
            YearMonth last = AHORA.minusMonths(1);
            history(
                    transaction(TransactionType.INCOME, "300000", last),
                    transaction(TransactionType.EXPENSE, "50000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result.get(0).getEstimatedIncome()).isEqualByComparingTo("100000.00");
            assertThat(result.get(0).getEstimatedExpense()).isEqualByComparingTo("16666.67");
            assertThat(result.get(0).getEstimatedNet()).isEqualByComparingTo("83333.33");
        }

        @Test
        @DisplayName("redondea a dos decimales")
        void redondeaADosDecimales() {
            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "100000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result.get(0).getEstimatedExpense().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("solo lee transacciones: las cuentas se saldoan en /balance")
        void soloLeeTransacciones() {
            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.INCOME, "300000", last));

            reportService.projections(userAuth, 3);

            verify(transactionRepository).findByUserIdAndDateBetween(
                    any(), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("historial vacio")
    class HistorialVacio {

        @Test
        @DisplayName("devuelve lista vacia, no ceros ni una division por cero")
        void devuelveListaVacia() {
            assertThat(reportService.projections(userAuth, 3)).isEmpty();
        }
    }

    @Nested
    @DisplayName("proyeccion de meses futuros")
    class ProyeccionDeMesesFuturos {

        @Test
        @DisplayName("devuelve los meses pedidos arrancando en el que sigue")
        void devuelveLosMesesPedidos() {
            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.INCOME, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 3);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getMonth()).isEqualTo(AHORA.plusMonths(1).toString());
            assertThat(result.get(1).getMonth()).isEqualTo(AHORA.plusMonths(2).toString());
            assertThat(result.get(2).getMonth()).isEqualTo(AHORA.plusMonths(3).toString());
        }

        @Test
        @DisplayName("el gasto fijo no se suma aparte: su frecuencia es texto libre")
        void noSumaLosGastosFijos() {
            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 12);

            assertThat(result).hasSize(12);
            assertThat(result).allSatisfy(p ->
                    assertThat(p.getEstimatedExpense()).isEqualByComparingTo("100000.00"));
        }

        @Test
        @DisplayName("correr al ultimo segundo del mes no cambia la ventana ni el promedio")
        void cruceDeFinDeMes() {
            reportService.setClock(Clock.fixed(
                    AHORA.atDay(30).atTime(23, 59, 59).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.INCOME, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMonth()).isEqualTo("2026-07");
            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(LocalDate.parse("2026-03-01")),
                    eq(LocalDate.parse("2026-05-31")));
        }

        @Test
        @DisplayName("correr al primer instante del mes siguiente corta la historia un mes")
        void cruceDeInicioDeMes() {
            reportService.setClock(Clock.fixed(
                    AHORA.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

            YearMonth last = AHORA.minusMonths(1);
            history(transaction(TransactionType.INCOME, "300000", last));

            reportService.projections(userAuth, 1);

            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(LocalDate.parse("2026-04-01")),
                    eq(LocalDate.parse("2026-06-30")));
        }
    }
}