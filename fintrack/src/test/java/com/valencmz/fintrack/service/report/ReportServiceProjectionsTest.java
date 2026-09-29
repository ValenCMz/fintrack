package com.valencmz.fintrack.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

@DisplayName("GET /api/reports/projections")
class ReportServiceProjectionsTest {

    private static final int HISTORY_MONTHS = 3;

    private TransactionRepository transactionRepository;
    private ReportService reportService;
    private UserAuth userAuth;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of());

        reportService = new ReportService();
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
            YearMonth last = YearMonth.now().minusMonths(1);
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
            YearMonth last = YearMonth.now().minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            // un solo mes cargado: el promedio conservador es un tercio
            assertThat(result.get(0).getEstimatedExpense()).isEqualByComparingTo("100000.00");
        }

        @Test
        @DisplayName("el filtro de fechas abarca los 3 meses cerrados completos")
        void elFiltroAbarcaTresMesesCerrados() {
            YearMonth thisMonth = YearMonth.now();
            YearMonth firstMonth = thisMonth.minusMonths(HISTORY_MONTHS);

            history(transaction(TransactionType.INCOME, "300000", thisMonth.minusMonths(1)));
            reportService.projections(userAuth, 3);

            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(firstMonth.atDay(1)),
                    eq(thisMonth.atDay(1).minusDays(1)));
        }

        @Test
        @DisplayName("el historico excluye el mes en curso por la ventana de fechas")
        void elHistoricoExcluyeElMesEnCurso() {
            YearMonth thisMonth = YearMonth.now();
            history(transaction(TransactionType.INCOME, "300000", thisMonth.minusMonths(1)));

            reportService.projections(userAuth, 1);

            // La exclusion del mes en curso no la hace el service: la hace la query,
            // cuya ventana termina el ultimo dia del mes anterior. Si esa ventana
            // cambia, el promedio empieza a contaminarse con el mes corriente.
            verify(transactionRepository).findByUserIdAndDateBetween(
                    eq(userAuth.getUser().getId()),
                    eq(thisMonth.minusMonths(HISTORY_MONTHS).atDay(1)),
                    eq(thisMonth.atDay(1).minusDays(1)));
        }

        @Test
        @DisplayName("separa ingresos de gastos")
        void separaIngresosDeGastos() {
            YearMonth last = YearMonth.now().minusMonths(1);
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
            YearMonth last = YearMonth.now().minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "100000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 1);

            assertThat(result.get(0).getEstimatedExpense().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("solo lee transacciones: las cuentas se saldoan en /balance")
        void soloLeeTransacciones() {
            YearMonth last = YearMonth.now().minusMonths(1);
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
            YearMonth last = YearMonth.now().minusMonths(1);
            history(transaction(TransactionType.INCOME, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 3);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getMonth()).isEqualTo(YearMonth.now().plusMonths(1).toString());
            assertThat(result.get(1).getMonth()).isEqualTo(YearMonth.now().plusMonths(2).toString());
            assertThat(result.get(2).getMonth()).isEqualTo(YearMonth.now().plusMonths(3).toString());
        }

        @Test
        @DisplayName("el gasto fijo no se suma aparte: su frecuencia es texto libre")
        void noSumaLosGastosFijos() {
            YearMonth last = YearMonth.now().minusMonths(1);
            history(transaction(TransactionType.EXPENSE, "300000", last));

            List<ProjectionResponse> result = reportService.projections(userAuth, 12);

            // solo entran los pagos ya registrados como transacciones
            assertThat(result).hasSize(12);
            assertThat(result).allSatisfy(p ->
                    assertThat(p.getEstimatedExpense()).isEqualByComparingTo("100000.00"));
        }
    }
}
