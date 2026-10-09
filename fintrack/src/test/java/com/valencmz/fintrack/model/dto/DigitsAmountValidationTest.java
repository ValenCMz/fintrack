package com.valencmz.fintrack.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.valencmz.fintrack.enums.DebtStatus;
import com.valencmz.fintrack.enums.MonotributoStatus;
import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.model.dto.card.CardRequest;
import com.valencmz.fintrack.model.dto.debt.DebtRequest;
import com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest;
import com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest;
import com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest;
import com.valencmz.fintrack.model.dto.transaction.TransactionRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Los 8 montos son numeric(38,2) en la base, o sea hasta 36 digitos enteros y
 * 2 decimales. Antes de @Digits un amount con 40 digitos reventaba contra la
 * base y llegaba al GlobalExceptionHandler como un 409 sin clasificar en vez
 * de ser rechazado con un 400 de validacion.
 */
@DisplayName("Los 8 montos respetan numeric(38,2): 36 enteros y 2 decimales")
class DigitsAmountValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private static final String[] MONTOS = {
            "com.valencmz.fintrack.model.dto.transaction.TransactionRequest#amount",
            "com.valencmz.fintrack.model.dto.card.CardRequest#amount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#totalAmount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#remainingAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#targetAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#currentAmount",
            "com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest#monthlyAmount",
            "com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest#amount" };

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "100000.999", "0.001" })
    @DisplayName("más de 2 decimales se rechaza, no se trunca")
    void rechazaMasDeDosDecimales(String value) {
        for (String referencia : MONTOS) {
            Object dto = construirValido(referencia);
            set(dto, campo(referencia), new BigDecimal(value));

            assertThat(VALIDATOR.validate(dto))
                    .as("%s con %s", referencia, value)
                    .anyMatch(v -> v.getPropertyPath().toString().equals(campo(referencia)));
        }
    }

    @Test
    @DisplayName("37 digitos enteros se rechazan: la columna aguanta 36")
    void rechazaTreintaYSieteEnteros() {
        String gigante = "1".repeat(37);

        for (String referencia : MONTOS) {
            Object dto = construirValido(referencia);
            set(dto, campo(referencia), new BigDecimal(gigante));

            assertThat(VALIDATOR.validate(dto))
                    .as("%s con 37 digitos", referencia)
                    .anyMatch(v -> v.getPropertyPath().toString().equals(campo(referencia)));
        }
    }

    @Test
    @DisplayName("36 digitos enteros con 2 decimales es el tope y entra")
    void aceptaElTopeDeLaColumna() {
        String tope = "1".repeat(36) + ".99";

        for (String referencia : MONTOS) {
            Object dto = construirValido(referencia);
            set(dto, campo(referencia), new BigDecimal(tope));

            assertThat(VALIDATOR.validate(dto))
                    .as("%s con el tope de numeric(38,2)", referencia)
                    .noneMatch(v -> v.getPropertyPath().toString().equals(campo(referencia)));
        }
    }

    @Test
    @DisplayName("los 8 montos conservan @Digits(integer=36, fraction=2)")
    void losOchoConservanDigits() {
        for (String referencia : MONTOS) {
            Field f = campoField(referencia);

            var digits = f.getAnnotation(jakarta.validation.constraints.Digits.class);

            assertThat(digits)
                    .as("%s debe conservar @Digits", referencia)
                    .isNotNull();
            assertThat(digits.integer()).as("%s integer", referencia).isEqualTo(36);
            assertThat(digits.fraction()).as("%s fraction", referencia).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("un DTO completo con montos normales sigue sin violaciones")
    void montosNormalesSiguenEntrando() {
        assertThat(VALIDATOR.validate(validCard())).isEmpty();
        assertThat(VALIDATOR.validate(validDebt())).isEmpty();
        assertThat(VALIDATOR.validate(validSavingGoal())).isEmpty();
        assertThat(VALIDATOR.validate(validTransaction())).isEmpty();
        assertThat(VALIDATOR.validate(validMonotributo())).isEmpty();
        assertThat(VALIDATOR.validate(validFixedExpende())).isEmpty();
    }

    @Test
    @DisplayName("0 sigue siendo valido en currentAmount y remainingAmount")
    void ceroSigueSiendoValidoEnLosMontosDeProgreso() {
        SavingGoalRequest goal = validSavingGoal();
        goal.setCurrentAmount(BigDecimal.ZERO);
        assertThat(VALIDATOR.validate(goal))
                .noneMatch(v -> v.getPropertyPath().toString().equals("currentAmount"));

        DebtRequest debt = validDebt();
        debt.setRemainingAmount(BigDecimal.ZERO);
        assertThat(VALIDATOR.validate(debt))
                .noneMatch(v -> v.getPropertyPath().toString().equals("remainingAmount"));
    }

    private String campo(String referencia) {
        return referencia.substring(referencia.indexOf('#') + 1);
    }

    private void set(Object dto, String field, Object value) {
        try {
            Field f = dto.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(dto, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Field campoField(String referencia) {
        try {
            String className = referencia.substring(0, referencia.indexOf('#'));
            return Class.forName(className).getDeclaredField(campo(referencia));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object construirValido(String referencia) {
        String className = referencia.substring(0, referencia.indexOf('#'));
        return switch (className) {
            case "com.valencmz.fintrack.model.dto.card.CardRequest" -> validCard();
            case "com.valencmz.fintrack.model.dto.debt.DebtRequest" -> validDebt();
            case "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest" -> validSavingGoal();
            case "com.valencmz.fintrack.model.dto.transaction.TransactionRequest" -> validTransaction();
            case "com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest" -> validMonotributo();
            case "com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest" -> validFixedExpende();
            default -> throw new IllegalArgumentException("DTO desconocido: " + className);
        };
    }

    private CardRequest validCard() {
        CardRequest dto = new CardRequest();
        dto.setHolderName("Valentin");
        dto.setDueDay(java.time.LocalDate.now().plusDays(10));
        dto.setAmount(new BigDecimal("50000"));
        dto.setActive(true);
        return dto;
    }

    private DebtRequest validDebt() {
        DebtRequest dto = new DebtRequest();
        dto.setCreditor("Banco");
        dto.setTotalAmount(new BigDecimal("50000"));
        dto.setRemainingAmount(new BigDecimal("50000"));
        dto.setStartDate(java.time.LocalDate.now().minusDays(10));
        dto.setStatus(DebtStatus.ACTIVE);
        return dto;
    }

    private SavingGoalRequest validSavingGoal() {
        SavingGoalRequest dto = new SavingGoalRequest();
        dto.setName("Fondo");
        dto.setTargetAmount(new BigDecimal("300000"));
        dto.setCurrentAmount(new BigDecimal("0"));
        dto.setTargetDate(java.time.LocalDate.now().plusDays(100));
        dto.setActive(true);
        return dto;
    }

    private TransactionRequest validTransaction() {
        TransactionRequest dto = new TransactionRequest();
        dto.setType(TransactionType.EXPENSE);
        dto.setDescription("Alquiler");
        dto.setAmount(new BigDecimal("80000"));
        dto.setDate(java.time.LocalDate.now());
        dto.setCategoryId(UUID.randomUUID());
        dto.setAccountId(UUID.randomUUID());
        return dto;
    }

    private MonotributoRequest validMonotributo() {
        MonotributoRequest dto = new MonotributoRequest();
        dto.setName("Cuota");
        dto.setMonthlyAmount(new BigDecimal("55000"));
        dto.setDueDay(java.time.LocalDate.now().plusDays(5));
        dto.setStatus(MonotributoStatus.DEBT);
        return dto;
    }

    private FixedExpendeRequest validFixedExpende() {
        FixedExpendeRequest dto = new FixedExpendeRequest();
        dto.setName("Alquiler");
        dto.setAmount(new BigDecimal("80000"));
        dto.setDueDay(java.time.LocalDate.now().plusDays(5));
        dto.setFrequency("mensual");
        dto.setActive(true);
        dto.setCategoryId(UUID.randomUUID());
        dto.setAccountId(UUID.randomUUID());
        return dto;
    }
}