package com.dgranda.nominaec.dto;

import com.dgranda.nominaec.entity.SettlementReason;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record SettlementRequest(
            @NotNull Long employeeId,
            @NotNull LocalDate terminationDate,
            @NotNull SettlementReason reason,
            @DecimalMin("0") @DecimalMax("30") @Digits(integer = 2, fraction = 2) BigDecimal pendingSalaryDays,
            @DecimalMin("0") @DecimalMax("120") @Digits(integer = 3, fraction = 2) BigDecimal unusedVacationDays) {
    }

    public record SettlementItemResponse(String code, String description, BigDecimal amount) {
    }

    public record SettlementResponse(Long id, Long employeeId, String employeeName, LocalDate startDate,
                                     LocalDate terminationDate, SettlementReason reason, int serviceDays,
                                     BigDecimal serviceYears, BigDecimal lastSalary, List<SettlementItemResponse> items,
                                     BigDecimal totalIncome, BigDecimal totalDeductions, BigDecimal netTotal,
                                     OffsetDateTime createdAt) {
    }
}
