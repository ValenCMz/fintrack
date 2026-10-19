package com.valencmz.fintrack.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.valencmz.fintrack.enums.AccountType;
import com.valencmz.fintrack.enums.CategoryType;
import com.valencmz.fintrack.enums.DebtStatus;
import com.valencmz.fintrack.enums.MonotributoStatus;
import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.model.dto.account.AccountRequest;
import com.valencmz.fintrack.model.dto.card.CardRequest;
import com.valencmz.fintrack.model.dto.category.CategoryRequest;
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
 * Antes de @Size un nombre de cuenta de 10.000 caracteres se guardaba sin
 * problema. Aca fijamos un tope generoso y verificamos que ademas el unicode
 * (acentos, emojis, CJK) siga entrando para no regresionar algo que es valido
 * en la vida real.
 */
@DisplayName("Strings validados con @Size: topes razonables y unicode intacto")
class SizeValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private static final String UNICODE = "Cuénta 🧉 日本語 Vélez";

    @Test
    @DisplayName("un nombre de cuenta de 10.000 caracteres se rechaza")
    void unNombreDeDiezMilSeRechaza() {
        AccountRequest dto = validAccount();
        dto.setName("a".repeat(10_000));

        assertThat(VALIDATOR.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("los campos de texto libre con nombres largos se rechazan por @Size")
    void camposLargosSeRechazan() {
        assertThat(VALIDATOR.validate(conNombreLargo(validAccount(), "name"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validCategory(), "name"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validCard(), "holderName"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validDebt(), "creditor"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validSavingGoal(), "name"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validMonotributo(), "name"))).isNotEmpty();
        assertThat(VALIDATOR.validate(conNombreLargo(validFixedExpende(), "name"))).isNotEmpty();
    }

    @Test
    @DisplayName("la descripcion de una transaccion tiene tope 255 y notes 1000")
    void descripcionYNombresLargos() {
        TransactionRequest tx = validTransaction();
        tx.setDescription("d".repeat(256));
        assertThat(VALIDATOR.validate(tx))
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"));

        tx = validTransaction();
        tx.setDescription("d".repeat(255));
        assertThat(VALIDATOR.validate(tx))
                .noneMatch(v -> v.getPropertyPath().toString().equals("description"));

        tx = validTransaction();
        tx.setNotes("n".repeat(1001));
        assertThat(VALIDATOR.validate(tx))
                .anyMatch(v -> v.getPropertyPath().toString().equals("notes"));
    }

    @Test
    @DisplayName("el color de categoria y la frecuencia tienen tope corto")
    void colorYFrecuencia() {
        CategoryRequest category = validCategory();
        category.setColor("#".concat("0".repeat(30)));
        assertThat(VALIDATOR.validate(category))
                .anyMatch(v -> v.getPropertyPath().toString().equals("color"));

        FixedExpendeRequest fixed = validFixedExpende();
        fixed.setFrequency("m".repeat(51));
        assertThat(VALIDATOR.validate(fixed))
                .anyMatch(v -> v.getPropertyPath().toString().equals("frequency"));
    }

    @Test
    @DisplayName("nombres con acentos, emojis y CJK de largo razonable se aceptan")
    void unicodeSigueEntrando() {
        AccountRequest account = validAccount();
        account.setName(UNICODE);
        account.setOwner(UNICODE);
        assertThat(VALIDATOR.validate(account)).isEmpty();

        CategoryRequest category = validCategory();
        category.setName(UNICODE);
        assertThat(VALIDATOR.validate(category)).isEmpty();

        TransactionRequest tx = validTransaction();
        tx.setDescription(UNICODE);
        tx.setNotes(UNICODE);
        assertThat(VALIDATOR.validate(tx)).isEmpty();
    }

    @Test
    @DisplayName("los campos opcionales siguen aceptando null")
    void losOpcionalesAceptanNull() {
        AccountRequest account = validAccount();
        account.setOwner(null);
        assertThat(VALIDATOR.validate(account)).isEmpty();

        TransactionRequest tx = validTransaction();
        tx.setNotes(null);
        assertThat(VALIDATOR.validate(tx)).isEmpty();

        FixedExpendeRequest fixed = validFixedExpende();
        fixed.setFrequency(null);
        assertThat(VALIDATOR.validate(fixed)).isEmpty();
    }

    @Test
    @DisplayName("los nombres de los recursos conservan @Size con topes generosos")
    void losCamposConservanSize() {
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.account.AccountRequest", "name")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.account.AccountRequest", "owner")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.category.CategoryRequest", "name")).isEqualTo(80);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.card.CardRequest", "holderName")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.debt.DebtRequest", "creditor")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.savinggoal.SavingGoalRequest", "name")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.monotributo.MonotributoRequest", "name")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest", "name")).isEqualTo(120);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.fixedexpense.FixedExpendeRequest", "frequency")).isEqualTo(50);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.transaction.TransactionRequest", "description")).isEqualTo(255);
        assertThat(sizeMax("com.valencmz.fintrack.model.dto.transaction.TransactionRequest", "notes")).isEqualTo(1000);
    }

    private int sizeMax(String className, String fieldName) {
        try {
            Field f = Class.forName(className).getDeclaredField(fieldName);
            var size = f.getAnnotation(jakarta.validation.constraints.Size.class);
            assertThat(size).as("%s debe conservar @Size", className + "#" + fieldName).isNotNull();
            return size.max();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object conNombreLargo(Object dto, String field) {
        try {
            Field f = dto.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(dto, "x".repeat(10_000));
            return dto;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private AccountRequest validAccount() {
        AccountRequest dto = new AccountRequest();
        dto.setName("Santander");
        dto.setType(AccountType.BANK);
        dto.setOwner("Valentin");
        dto.setActive(true);
        return dto;
    }

    private CategoryRequest validCategory() {
        CategoryRequest dto = new CategoryRequest();
        dto.setName("Alquiler");
        dto.setColor("#FF0000");
        dto.setType(CategoryType.EXPENSE);
        dto.setActive(true);
        return dto;
    }

    private CardRequest validCard() {
        CardRequest dto = new CardRequest();
        dto.setHolderName("Valentin");
        dto.setDueDayOfMonth(10);
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