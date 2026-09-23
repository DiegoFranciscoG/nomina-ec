package com.dgranda.nominaec.dto;

import com.dgranda.nominaec.entity.AuditAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class LegalParameterDtos {

    private LegalParameterDtos() {
    }

    public record LegalParameterResponse(Long id, String code, BigDecimal value, LocalDate validFrom, LocalDate validTo,
                                         String description, String legalBasis, String sourceUrl, String createdBy,
                                         boolean inForce) {
    }

    /** Creates a new version of a parameter; the previous open-ended version is closed the day before. */
    public record NewVersionRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,60}$") String code,
            @NotNull BigDecimal value,
            @NotNull LocalDate validFrom,
            @Size(max = 255) String description,
            @NotBlank @Size(max = 255) String legalBasis,
            @NotBlank @Size(max = 500) @URL(protocol = "https") String sourceUrl,
            @NotBlank @Size(min = 10, max = 500) String reason) {
    }

    public record AuditResponse(Long id, Long parameterId, String code, AuditAction action, Map<String, Object> oldValue,
                                Map<String, Object> newValue, String reason, String changedBy, OffsetDateTime changedAt) {
    }

    public record TaxBracketResponse(int fiscalYear, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal baseTax,
                                     BigDecimal marginalRate, String sourceUrl) {
    }

    public record ExpenseCapResponse(int fiscalYear, int familyDependents, BigDecimal basketMultiplier, BigDecimal maxAmount,
                                     BigDecimal maxRebate) {
    }

    public record TaxTablesResponse(int fiscalYear, List<TaxBracketResponse> brackets, List<ExpenseCapResponse> personalExpenseCaps) {
    }
}
