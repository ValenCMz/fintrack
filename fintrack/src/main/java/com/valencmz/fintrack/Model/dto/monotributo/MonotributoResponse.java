package com.valencmz.fintrack.model.dto.monotributo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.valencmz.fintrack.enums.MonotributoStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonotributoResponse {
    private UUID id;
    private String name;
    private BigDecimal monthlyAmount;
    private LocalDate dueDay;
    private MonotributoStatus status;
}
