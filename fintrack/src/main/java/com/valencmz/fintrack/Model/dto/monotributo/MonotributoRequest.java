package com.valencmz.fintrack.model.dto.monotributo;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.valencmz.fintrack.enums.MonotributoStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonotributoRequest {
    private String name;
    private BigDecimal monthlyAmount;
    private LocalDate dueDay;
    private MonotributoStatus status;
}
