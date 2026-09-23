package com.dgranda.nominaec.mapper;

import com.dgranda.nominaec.calculation.Money;
import com.dgranda.nominaec.dto.PayrollDtos.NoveltyResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayrollSheetResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipLineResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipSummary;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodResponse;
import com.dgranda.nominaec.dto.PayrollDtos.ProvisionResponse;
import com.dgranda.nominaec.dto.PayrollDtos.Totals;
import com.dgranda.nominaec.entity.Concept;
import com.dgranda.nominaec.entity.ConceptType;
import com.dgranda.nominaec.entity.Novelty;
import com.dgranda.nominaec.entity.PayrollPeriod;
import com.dgranda.nominaec.entity.Payslip;
import com.dgranda.nominaec.entity.PayslipLine;
import com.dgranda.nominaec.repository.ConceptRepository;
import com.dgranda.nominaec.service.validation.CedulaValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
public class PayrollMapper {

    private final ConceptRepository conceptRepository;
    private final Map<String, Concept> concepts = new ConcurrentHashMap<>();

    public PayrollMapper(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public Concept concept(String code) {
        if (concepts.isEmpty()) {
            conceptRepository.findAll().forEach(c -> concepts.put(c.getCode(), c));
        }
        return concepts.get(code);
    }

    public PeriodResponse toResponse(PayrollPeriod p, long payslipCount) {
        return new PeriodResponse(p.getId(), p.getYear(), p.getMonth(), p.getStatus(), p.getCalculatedAt(), p.getClosedAt(),
                p.getClosedBy(), payslipCount);
    }

    public NoveltyResponse toResponse(Novelty n) {
        return new NoveltyResponse(n.getId(), n.getEmployee().getId(), EmployeeMapper.fullName(n.getEmployee()),
                n.getNoveltyType(), n.getQuantity(), n.getAmount(), n.getDescription());
    }

    public PayslipLineResponse toResponse(PayslipLine l) {
        Concept c = concept(l.getConceptCode());
        return new PayslipLineResponse(l.getConceptCode(), c == null ? l.getConceptCode() : c.getName(),
                c == null ? null : c.getConceptType(), l.getQuantity(), l.getRate(), l.getAmount());
    }

    public PayslipSummary toSummary(Payslip p) {
        return new PayslipSummary(p.getId(), p.getEmployee().getId(), EmployeeMapper.fullName(p.getEmployee()),
                p.getContract().getPosition().getName(), p.getWorkedDays(), p.getBaseSalary(), p.getIessBase(),
                p.getTotalIncome(), p.getTotalDeductions(), p.getNetPay(), p.getEmployerCost());
    }

    public PayslipResponse toResponse(Payslip p) {
        return new PayslipResponse(p.getId(), p.getPeriod().getYear(), p.getPeriod().getMonth(), p.getEmployee().getId(),
                EmployeeMapper.fullName(p.getEmployee()), CedulaValidator.mask(p.getEmployee().getIdNumber()),
                p.getContract().getPosition().getName(), p.getWorkedDays(), p.getBaseSalary(), p.getIessBase(),
                p.getIncomeTaxBase(), p.getProjectedAnnualTaxBase(),
                p.getLines().stream().map(this::toResponse).toList(),
                p.getProvisions().stream().map(v -> new ProvisionResponse(v.getProvisionType(), v.getAmount(), v.isPaidMonthly())).toList(),
                p.getTotalIncome(), p.getTotalDeductions(), p.getNetPay(), p.getEmployerCost(), p.getParametersSnapshot());
    }

    public PayrollSheetResponse toSheet(PeriodResponse period, List<Payslip> payslips) {
        List<PayslipSummary> rows = payslips.stream().map(this::toSummary).toList();
        Totals totals = new Totals(
                total(payslips, Payslip::getTotalIncome),
                total(payslips, Payslip::getTotalDeductions),
                total(payslips, Payslip::getNetPay),
                lines(payslips, l -> l.getConceptCode().equals("IESS_PERSONAL")),
                lines(payslips, l -> l.getConceptCode().equals("IESS_EMPLOYER")),
                lines(payslips, l -> l.getConceptCode().equals("INCOME_TAX")),
                lines(payslips, l -> {
                    Concept c = concept(l.getConceptCode());
                    return c != null && c.getConceptType() == ConceptType.PROVISION;
                }),
                total(payslips, Payslip::getEmployerCost));
        return new PayrollSheetResponse(period, rows, totals);
    }

    private static BigDecimal total(List<Payslip> payslips, java.util.function.Function<Payslip, BigDecimal> f) {
        return payslips.stream().map(f).reduce(Money.ZERO, BigDecimal::add);
    }

    private static BigDecimal lines(List<Payslip> payslips, Predicate<PayslipLine> filter) {
        return payslips.stream().flatMap(p -> p.getLines().stream()).filter(filter)
                .map(PayslipLine::getAmount).reduce(Money.ZERO, BigDecimal::add);
    }
}
