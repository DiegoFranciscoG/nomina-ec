package com.dgranda.nominaec.mapper;

import com.dgranda.nominaec.calculation.IncomeTaxCalculator;
import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.Money;
import com.dgranda.nominaec.calculation.ParameterCode;
import com.dgranda.nominaec.dto.LegalParameterDtos.AuditResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.ExpenseCapResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.LegalParameterResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.TaxBracketResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.TaxTablesResponse;
import com.dgranda.nominaec.entity.IncomeTaxBracket;
import com.dgranda.nominaec.entity.LegalParameter;
import com.dgranda.nominaec.entity.LegalParameterAudit;
import com.dgranda.nominaec.entity.PersonalExpenseCap;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LegalParameterMapper {

    public LegalParameterResponse toResponse(LegalParameter p, LocalDate today) {
        boolean inForce = !p.getValidFrom().isAfter(today) && (p.getValidTo() == null || !p.getValidTo().isBefore(today));
        return new LegalParameterResponse(p.getId(), p.getCode(), p.getValue().stripTrailingZeros(), p.getValidFrom(),
                p.getValidTo(), p.getDescription(), p.getLegalBasis(), p.getSourceUrl(), p.getCreatedBy(), inForce);
    }

    public Map<String, Object> toAuditValue(LegalParameter p) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("value", p.getValue().stripTrailingZeros().toPlainString());
        value.put("validFrom", p.getValidFrom().toString());
        value.put("validTo", p.getValidTo() == null ? null : p.getValidTo().toString());
        value.put("legalBasis", p.getLegalBasis());
        value.put("sourceUrl", p.getSourceUrl());
        return value;
    }

    public AuditResponse toAuditResponse(LegalParameterAudit a) {
        return new AuditResponse(a.getId(), a.getParameterId(), a.getCode(), a.getAction(), a.getOldValue(), a.getNewValue(),
                a.getReason(), a.getChangedBy(), a.getChangedAt());
    }

    public TaxTablesResponse toTaxTables(int year, List<IncomeTaxBracket> brackets, List<PersonalExpenseCap> caps,
                                         LegalParameterSet set) {
        List<TaxBracketResponse> rows = brackets.stream()
                .map(b -> new TaxBracketResponse(b.getFiscalYear(), b.getLowerBound(), b.getUpperBound(), b.getBaseTax(),
                        b.getMarginalRate(), b.getSourceUrl()))
                .toList();
        List<ExpenseCapResponse> capRows = caps.stream().map(c -> {
            var max = Money.round(IncomeTaxCalculator.personalExpenseCap(c.getFamilyDependents(), false, set));
            var rebate = Money.round(max.multiply(set.get(ParameterCode.PERSONAL_EXPENSE_REBATE_RATE)));
            return new ExpenseCapResponse(c.getFiscalYear(), c.getFamilyDependents(), c.getBasketMultiplier(), max, rebate);
        }).toList();
        return new TaxTablesResponse(year, rows, capRows);
    }
}
