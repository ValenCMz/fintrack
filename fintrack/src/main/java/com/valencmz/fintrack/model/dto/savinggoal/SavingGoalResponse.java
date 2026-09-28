package com.valencmz.fintrack.model.dto.savinggoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingGoalResponse {
    private UUID id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private boolean active;

    public SavingGoalResponse(com.valencmz.fintrack.model.entity.SavingGoal savingGoal) {
        this.id = savingGoal.getId();
        this.name = savingGoal.getName();
        this.targetAmount = savingGoal.getTargetAmount();
        this.currentAmount = savingGoal.getCurrentAmount();
        this.targetDate = savingGoal.getTargetDate();
        this.active = savingGoal.isActive();
    }
}
