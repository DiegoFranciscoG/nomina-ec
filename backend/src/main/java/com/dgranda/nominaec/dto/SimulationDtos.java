package com.dgranda.nominaec.dto;

import com.dgranda.nominaec.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SimulationDtos {

    private SimulationDtos() {
    }

    public record HiringCostRequest(
            @NotNull @DecimalMin("1.00") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal monthlySalary,
            @Min(1) @Max(40) int weeklyHours,
            LocalDate referenceDate,
            @NotNull PaymentMode thirteenthMode,
            @NotNull PaymentMode fourteenthMode) {
    }
}
