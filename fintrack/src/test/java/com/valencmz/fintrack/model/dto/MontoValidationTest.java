package com.valencmz.fintrack.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

@DisplayName("Los 8 montos con NOT NULL en la tabla rechazan null")
class MontoValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private static final List<String> NOT_NULL_AMOUNTS = List.of(
            "com.valencmz.fintrack.model.dto.transaction.TransactionRequest.amount",
            "com.valencmz.fintrack.model.dto.card.CardRequest.amount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest.totalAmount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest.remainingAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest.targetAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest.currentAmount",
            "com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest.monthlyAmount",
            "com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest.amount");

    @Test
    @DisplayName("son 8 montos y la lista del test no se quedó vieja")
    void sonOchoMontos() {
        assertThat(NOT_NULL_AMOUNTS).hasSize(8);
    }

    @ParameterizedTest(name = "{0} rechaza null")
    @ValueSource(strings = {
            "com.valencmz.fintrack.model.dto.transaction.TransactionRequest#amount",
            "com.valencmz.fintrack.model.dto.card.CardRequest#amount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#totalAmount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#remainingAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#targetAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#currentAmount",
            "com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest#monthlyAmount",
            "com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest#amount" })
    @DisplayName("rechaza null con la constraint NotNull")
    void rechazaNull(String reference) {
        Object dto = newDto(reference.substring(0, reference.indexOf('#')));
        String field = reference.substring(reference.indexOf('#') + 1);
        set(dto, field, null);

        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(dto);

        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo(field);
                    assertThat(v.getMessage()).isEqualTo("no debe ser nulo");
                });
    }

    @ParameterizedTest(name = "{0} conserva @NotNull")
    @ValueSource(strings = {
            "com.valencmz.fintrack.model.dto.transaction.TransactionRequest#amount",
            "com.valencmz.fintrack.model.dto.card.CardRequest#amount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#totalAmount",
            "com.valencmz.fintrack.model.dto.debt.DebtRequest#remainingAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#targetAmount",
            "com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest#currentAmount",
            "com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest#monthlyAmount",
            "com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest#amount" })
    @DisplayName("el monto sigue declarado NotNull, no quedo como Positive solo")
    void conservaNotNull(String reference) {
        String className = reference.substring(0, reference.indexOf('#'));
        String fieldName = reference.substring(reference.indexOf('#') + 1);

        Field field = field(className, fieldName);

        assertThat(field.getAnnotation(jakarta.validation.constraints.NotNull.class))
                .as("%s debe seguir con @NotNull", reference)
                .isNotNull();
    }

    @Test
    @DisplayName("remainingAmount acepta 0: una deuda saldada es valida")
    void remainingAmountAceptaCero() {
        DebtRequest dto = validDebt();
        dto.setRemainingAmount(BigDecimal.ZERO);

        assertThat(VALIDATOR.validate(dto))
                .noneMatch(v -> v.getPropertyPath().toString().equals("remainingAmount"));
    }

    @Test
    @DisplayName("currentAmount acepta 0: una meta sin ahorrar es valida")
    void currentAmountAceptaCero() {
        SavingGoalRequest dto = validSavingGoal();
        dto.setCurrentAmount(BigDecimal.ZERO);

        assertThat(VALIDATOR.validate(dto))
                .noneMatch(v -> v.getPropertyPath().toString().equals("currentAmount"));
    }

    @Test
    @DisplayName("un amount null se rechaza aunque el resto del DTO este completo")
    void amountNullSeRechaza() {
        CardRequest dto = validCard();
        dto.setAmount(null);

        assertThat(VALIDATOR.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "-0.01" })
    @DisplayName("amount, totalAmount y targetAmount rechazan cero y negativos")
    void rechazaCeroYNegativos(String value) {
        CardRequest card = validCard();
        card.setAmount(new BigDecimal(value));

        DebtRequest debt = validDebt();
        debt.setTotalAmount(new BigDecimal(value));

        SavingGoalRequest goal = validSavingGoal();
        goal.setTargetAmount(new BigDecimal(value));

        TransactionRequest tx = validTransaction();
        tx.setAmount(new BigDecimal(value));

        assertThat(VALIDATOR.validate(card)).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
        assertThat(VALIDATOR.validate(debt)).anyMatch(v -> v.getPropertyPath().toString().equals("totalAmount"));
        assertThat(VALIDATOR.validate(goal)).anyMatch(v -> v.getPropertyPath().toString().equals("targetAmount"));
        assertThat(VALIDATOR.validate(tx)).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("un DTO completo y valido no produce violaciones")
    void unDtoCompletoNoProduceViolaciones() {
        assertThat(VALIDATOR.validate(validCard())).isEmpty();
        assertThat(VALIDATOR.validate(validDebt())).isEmpty();
        assertThat(VALIDATOR.validate(validSavingGoal())).isEmpty();
        assertThat(VALIDATOR.validate(validTransaction())).isEmpty();
        assertThat(VALIDATOR.validate(validMonotributo())).isEmpty();
        assertThat(VALIDATOR.validate(validFixedExpende())).isEmpty();
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

    private Object newDto(String className) {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Field field(String className, String fieldName) {
        try {
            return Class.forName(className).getDeclaredField(fieldName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
        dto.setStatus(com.valencmz.fintrack.enums.DebtStatus.ACTIVE);
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
        dto.setType(com.valencmz.fintrack.enums.TransactionType.EXPENSE);
        dto.setDescription("Alquiler");
        dto.setAmount(new BigDecimal("80000"));
        dto.setDate(java.time.LocalDate.now());
        dto.setCategoryId(java.util.UUID.randomUUID());
        dto.setAccountId(java.util.UUID.randomUUID());
        return dto;
    }

    private MonotributoRequest validMonotributo() {
        MonotributoRequest dto = new MonotributoRequest();
        dto.setName("Cuota");
        dto.setMonthlyAmount(new BigDecimal("55000"));
        dto.setDueDay(java.time.LocalDate.now().plusDays(5));
        dto.setStatus(com.valencmz.fintrack.enums.MonotributoStatus.DEBT);
        return dto;
    }

    private FixedExpendeRequest validFixedExpende() {
        FixedExpendeRequest dto = new FixedExpendeRequest();
        dto.setName("Alquiler");
        dto.setAmount(new BigDecimal("80000"));
        dto.setDueDay(java.time.LocalDate.now().plusDays(5));
        dto.setFrequency("mensual");
        dto.setActive(true);
        dto.setCategoryId(java.util.UUID.randomUUID());
        dto.setAccountId(java.util.UUID.randomUUID());
        return dto;
    }
}
