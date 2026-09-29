package com.valencmz.fintrack.model.dto.savinggoal;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.model.entity.SavingGoal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalRequest {
    @NotBlank
    private String name;
    @NotNull
    @Positive
    private BigDecimal targetAmount;
    @NotNull
    @PositiveOrZero
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
