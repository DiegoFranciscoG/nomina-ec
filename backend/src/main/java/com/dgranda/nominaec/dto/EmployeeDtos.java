package com.dgranda.nominaec.dto;

import com.dgranda.nominaec.entity.ContractStatus;
import com.dgranda.nominaec.entity.ContractType;
import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.Region;
import com.dgranda.nominaec.service.validation.ValidCedula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            @NotBlank @ValidCedula String idNumber,
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "solo letras") String firstNames,
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "solo letras") String lastNames,
            @Email @Size(max = 120) String email,
            @Min(0) @Max(20) int familyDependents,
            boolean catastrophicCondition,
            @Valid @NotNull ContractRequest contract) {
    }

    public record EmployeeUpdateRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "solo letras") String firstNames,
            @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L} .'-]+$", message = "solo letras") String lastNames,
            @Email @Size(max = 120) String email,
            @Min(0) @Max(20) int familyDependents,
            boolean catastrophicCondition) {
    }

    public record ContractRequest(
            @NotNull Long positionId,
            @NotNull ContractType contractType,
            @Min(1) @Max(40) int weeklyHours,
            @NotNull @DecimalMin("1.00") @Digits(integer = 9, fraction = 2) BigDecimal monthlySalary,
            @NotNull LocalDate startDate,
            @NotNull Region region,
            @NotNull PaymentMode thirteenthMode,
            @NotNull PaymentMode fourteenthMode,
            @NotNull PaymentMode reserveFundMode) {
    }

    public record ContractUpdateRequest(
            @NotNull @DecimalMin("1.00") @Digits(integer = 9, fraction = 2) BigDecimal monthlySalary,
            @Min(1) @Max(40) int weeklyHours,
            @NotNull PaymentMode thirteenthMode,
            @NotNull PaymentMode fourteenthMode,
            @NotNull PaymentMode reserveFundMode) {
    }

    public record PersonalExpenseRequest(
            @Min(2000) @Max(2100) int fiscalYear,
            @NotNull @DecimalMin("0.00") @Digits(integer = 9, fraction = 2) BigDecimal projectedAmount) {
    }

    /** Id number is masked (LOPDP data minimization). */
    public record EmployeeResponse(Long id, String idNumberMasked, String firstNames, String lastNames, String email,
                                   int familyDependents, boolean catastrophicCondition, boolean active,
                                   ContractResponse contract) {
    }

    public record ContractResponse(Long id, Long positionId, String positionName, ContractType contractType, int weeklyHours,
                                   BigDecimal monthlySalary, LocalDate startDate, LocalDate endDate, Region region,
                                   PaymentMode thirteenthMode, PaymentMode fourteenthMode, PaymentMode reserveFundMode,
                                   ContractStatus status) {
    }

    public record PositionResponse(Long id, String code, String name) {
    }

    public record BenefitBalanceResponse(Long employeeId, int year, BigDecimal thirteenthAccumulated, BigDecimal thirteenthPaid,
                                         BigDecimal fourteenthAccumulated, BigDecimal fourteenthPaid,
                                         BigDecimal reserveFundAccumulated, BigDecimal reserveFundPaid,
                                         BigDecimal vacationProvision) {
    }
}
