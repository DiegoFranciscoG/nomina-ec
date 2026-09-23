package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.ParameterCode;
import com.dgranda.nominaec.calculation.TaxBracket;
import com.dgranda.nominaec.dto.LegalParameterDtos.AuditResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.LegalParameterResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.NewVersionRequest;
import com.dgranda.nominaec.dto.LegalParameterDtos.TaxTablesResponse;
import com.dgranda.nominaec.entity.AuditAction;
import com.dgranda.nominaec.entity.LegalParameter;
import com.dgranda.nominaec.entity.LegalParameterAudit;
import com.dgranda.nominaec.entity.PeriodStatus;
import com.dgranda.nominaec.exception.BusinessRuleException;
import com.dgranda.nominaec.exception.ConflictException;
import com.dgranda.nominaec.mapper.LegalParameterMapper;
import com.dgranda.nominaec.repository.IncomeTaxBracketRepository;
import com.dgranda.nominaec.repository.LegalParameterAuditRepository;
import com.dgranda.nominaec.repository.LegalParameterRepository;
import com.dgranda.nominaec.repository.PayrollPeriodRepository;
import com.dgranda.nominaec.repository.PersonalExpenseCapRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date-versioned legal parameters. Changing a value is an INSERT with a new validity range,
 * so no recompilation or redeploy is needed, and past payslips keep their original values.
 */
@Service
public class LegalParameterService {

    private final LegalParameterRepository parameters;
    private final LegalParameterAuditRepository audits;
    private final IncomeTaxBracketRepository brackets;
    private final PersonalExpenseCapRepository caps;
    private final PayrollPeriodRepository periods;
    private final LegalParameterMapper mapper;

    public LegalParameterService(LegalParameterRepository parameters, LegalParameterAuditRepository audits,
                                 IncomeTaxBracketRepository brackets, PersonalExpenseCapRepository caps,
                                 PayrollPeriodRepository periods, LegalParameterMapper mapper) {
        this.parameters = parameters;
        this.audits = audits;
        this.brackets = brackets;
        this.caps = caps;
        this.periods = periods;
        this.mapper = mapper;
    }

    /** Builds the immutable parameter set used by the calculation engine for the given date. */
    @Transactional(readOnly = true)
    public LegalParameterSet loadSet(LocalDate date) {
        Map<String, ParameterCode> known = Arrays.stream(ParameterCode.values())
                .collect(Collectors.toMap(Enum::name, c -> c));
        Map<ParameterCode, BigDecimal> values = new EnumMap<>(ParameterCode.class);
        for (LegalParameter p : parameters.findInForceOn(date)) {
            ParameterCode code = known.get(p.getCode());
            if (code != null) {
                values.put(code, p.getValue());
            }
        }
        List<TaxBracket> taxBrackets = brackets.findByFiscalYearOrderByLowerBound(date.getYear()).stream()
                .map(b -> new TaxBracket(b.getLowerBound(), b.getUpperBound(), b.getBaseTax(), b.getMarginalRate()))
                .toList();
        Map<Integer, BigDecimal> expenseCaps = new LinkedHashMap<>();
        caps.findByFiscalYearOrderByFamilyDependents(date.getYear())
                .forEach(c -> expenseCaps.put(c.getFamilyDependents(), c.getBasketMultiplier()));
        return new LegalParameterSet(date, values, taxBrackets, expenseCaps);
    }

    @Transactional(readOnly = true)
    public List<LegalParameterResponse> list(LocalDate today) {
        return parameters.findAllByOrderByCodeAscValidFromDesc().stream().map(p -> mapper.toResponse(p, today)).toList();
    }

    @Transactional(readOnly = true)
    public List<LegalParameterResponse> history(String code, LocalDate today) {
        return parameters.findByCodeOrderByValidFromDesc(code).stream().map(p -> mapper.toResponse(p, today)).toList();
    }

    @Transactional(readOnly = true)
    public TaxTablesResponse taxTables(int fiscalYear) {
        LegalParameterSet set = loadSet(LocalDate.of(fiscalYear, 1, 1));
        return mapper.toTaxTables(fiscalYear, brackets.findByFiscalYearOrderByLowerBound(fiscalYear),
                caps.findByFiscalYearOrderByFamilyDependents(fiscalYear), set);
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> audit(String code, Pageable pageable) {
        Page<LegalParameterAudit> page = code == null || code.isBlank()
                ? audits.findAllByOrderByChangedAtDescIdDesc(pageable)
                : audits.findByCodeOrderByChangedAtDescIdDesc(code, pageable);
        return page.map(mapper::toAuditResponse);
    }

    /**
     * Registers a new version. The open-ended version of the same code (if any) is closed on
     * {@code validFrom - 1}. Rejected when it would alter a closed payroll period.
     */
    @Transactional
    public LegalParameterResponse createVersion(NewVersionRequest request, String username, LocalDate today) {
        if (periods.existsWithStatusFrom(PeriodStatus.CLOSED, request.validFrom().getYear(), request.validFrom().getMonthValue())) {
            throw new ConflictException("La vigencia " + request.validFrom() + " afecta un periodo de nómina cerrado; "
                    + "use una fecha posterior al último periodo cerrado");
        }
        List<LegalParameter> history = parameters.findByCodeOrderByValidFromDesc(request.code());
        LegalParameter current = parameters.findOpenEnded(request.code()).orElse(null);
        if (current != null) {
            if (!request.validFrom().isAfter(current.getValidFrom())) {
                throw new BusinessRuleException("La nueva vigencia debe empezar después del " + current.getValidFrom());
            }
            Map<String, Object> before = mapper.toAuditValue(current);
            current.setValidTo(request.validFrom().minusDays(1));
            parameters.saveAndFlush(current);
            audits.save(auditOf(current, AuditAction.CLOSE_VALIDITY, before, mapper.toAuditValue(current), request.reason(), username));
        } else if (history.isEmpty() && request.description() == null) {
            throw new BusinessRuleException("Un parámetro nuevo requiere descripción");
        }

        LegalParameter created = new LegalParameter();
        created.setCode(request.code());
        created.setValue(request.value());
        created.setValidFrom(request.validFrom());
        created.setDescription(request.description() != null ? request.description()
                : history.getFirst().getDescription());
        created.setLegalBasis(request.legalBasis());
        created.setSourceUrl(request.sourceUrl());
        created.setCreatedBy(username);
        created = parameters.saveAndFlush(created);
        audits.save(auditOf(created, AuditAction.CREATE, null, mapper.toAuditValue(created), request.reason(), username));
        return mapper.toResponse(created, today);
    }

    private static LegalParameterAudit auditOf(LegalParameter p, AuditAction action, Map<String, Object> before,
                                               Map<String, Object> after, String reason, String username) {
        LegalParameterAudit audit = new LegalParameterAudit();
        audit.setParameterId(p.getId());
        audit.setCode(p.getCode());
        audit.setAction(action);
        audit.setOldValue(before);
        audit.setNewValue(after);
        audit.setReason(reason);
        audit.setChangedBy(username);
        return audit;
    }
}
