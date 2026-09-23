package com.dgranda.nominaec.dto;

import com.dgranda.nominaec.entity.ConceptType;
import com.dgranda.nominaec.entity.NoveltyType;
import com.dgranda.nominaec.entity.PeriodStatus;
import com.dgranda.nominaec.entity.ProvisionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class PayrollDtos {

    private PayrollDtos() {
    }

    public record PeriodRequest(@Min(2026) @Max(2100) int year, @Min(1) @Max(12) int month) {
    }

    public record PeriodResponse(Long id, int year, int month, PeriodStatus status, OffsetDateTime calculatedAt,
                                 OffsetDateTime closedAt, String closedBy, long payslipCount) {
    }

    public record NoveltyRequest(
            @NotNull Long employeeId,
            @NotNull NoveltyType noveltyType,
            @DecimalMin("0.00") @DecimalMax("744.00") @Digits(integer = 6, fraction = 2) BigDecimal quantity,
            @DecimalMin("0.00") @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @Size(max = 255) String description) {
    }

    public record NoveltyResponse(Long id, Long employeeId, String employeeName, NoveltyType noveltyType,
                                  BigDecimal quantity, BigDecimal amount, String description) {
    }

    public record PayslipLineResponse(String conceptCode, String conceptName, ConceptType conceptType,
                                      BigDecimal quantity, BigDecimal rate, BigDecimal amount) {
    }

    public record ProvisionResponse(ProvisionType type, BigDecimal amount, boolean paidMonthly) {
    }

    public record PayslipSummary(Long id, Long employeeId, String employeeName, String positionName, BigDecimal workedDays,
                                 BigDecimal baseSalary, BigDecimal iessBase, BigDecimal totalIncome, BigDecimal totalDeductions,
                                 BigDecimal netPay, BigDecimal employerCost) {
    }

    public record PayslipResponse(Long id, int year, int month, Long employeeId, String employeeName, String idNumberMasked,
                                  String positionName, BigDecimal workedDays, BigDecimal baseSalary, BigDecimal iessBase,
                                  BigDecimal incomeTaxBase, BigDecimal projectedAnnualTaxBase, List<PayslipLineResponse> lines,
                                  List<ProvisionResponse> provisions, BigDecimal totalIncome, BigDecimal totalDeductions,
                                  BigDecimal netPay, BigDecimal employerCost, java.util.Map<String, Object> parametersSnapshot) {
    }

    public record PayrollSheetResponse(PeriodResponse period, List<PayslipSummary> payslips, Totals totals) {
    }

    public record Totals(BigDecimal totalIncome, BigDecimal totalDeductions, BigDecimal netPay, BigDecimal iessPersonal,
                         BigDecimal iessEmployer, BigDecimal incomeTax, BigDecimal provisions, BigDecimal employerCost) {
    }
}
