package com.valencmz.fintrack.model.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.model.entity.Card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    @NotBlank
    private String holderName;
    @NotNull
    private LocalDate dueDay;
    @NotNull
    @Positive
    private BigDecimal amount;
    private boolean active;

    public Card toEntity() {
        Card card = new Card();
        card.setHolderName(this.holderName);
        card.setDueDay(this.dueDay);
        card.setAmount(this.amount);
        card.setActive(this.active);
        return card;
    }
}
