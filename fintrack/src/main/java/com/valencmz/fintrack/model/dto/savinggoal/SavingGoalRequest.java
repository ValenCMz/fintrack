package com.valencmz.fintrack.model.dto.savinggoal;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.model.entity.SavingGoal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @NotNull
    @Positive
    @Digits(integer = 36, fraction = 2)
    private BigDecimal targetAmount;
    @NotNull
    @PositiveOrZero
    @Digits(integer = 36, fraction = 2)
    private BigDecimal currentAmount;
    @NotNull
    private LocalDate targetDate;
    private boolean active;

    public SavingGoal toEntity() {
        SavingGoal savingGoal = new SavingGoal();
        savingGoal.setName(this.name);
        savingGoal.setTargetAmount(this.targetAmount);
        savingGoal.setCurrentAmount(this.currentAmount);
        savingGoal.setTargetDate(this.targetDate);
        savingGoal.setActive(this.active);
        return savingGoal;
    }
}
