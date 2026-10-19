package com.valencmz.fintrack.model.dto.card;

import com.valencmz.fintrack.model.entity.Card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    @NotBlank
    @Size(max = 120)
    private String holderName;
    @Min(1)
    @Max(31)
    private int dueDayOfMonth;
    private boolean active;

    public Card toEntity() {
        Card card = new Card();
        card.setHolderName(this.holderName);
        card.setDueDayOfMonth(this.dueDayOfMonth);
        card.setActive(this.active);
        return card;
    }
}
