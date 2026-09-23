package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.PayrollCalculator;
import com.dgranda.nominaec.calculation.PayrollInput;
import com.dgranda.nominaec.calculation.PayrollResult;
import com.dgranda.nominaec.dto.PayrollDtos.NoveltyRequest;
import com.dgranda.nominaec.dto.PayrollDtos.NoveltyResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayrollSheetResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodRequest;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodResponse;
import com.dgranda.nominaec.entity.Contract;
import com.dgranda.nominaec.entity.Employee;
import com.dgranda.nominaec.entity.Novelty;
import com.dgranda.nominaec.entity.NoveltyType;
import com.dgranda.nominaec.entity.PayrollPeriod;
import com.dgranda.nominaec.entity.Payslip;
import com.dgranda.nominaec.entity.PayslipLine;
import com.dgranda.nominaec.entity.PeriodStatus;
import com.dgranda.nominaec.entity.Provision;
import com.dgranda.nominaec.exception.BusinessRuleException;
import com.dgranda.nominaec.exception.ConflictException;
import com.dgranda.nominaec.exception.NotFoundException;
import com.dgranda.nominaec.mapper.EmployeeMapper;
import com.dgranda.nominaec.mapper.PayrollMapper;
import com.dgranda.nominaec.repository.ContractRepository;
import com.dgranda.nominaec.repository.NoveltyRepository;
import com.dgranda.nominaec.repository.PayrollPeriodRepository;
import com.dgranda.nominaec.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payroll periods: novelties, calculation, closing and the payroll sheet.
 * A CLOSED period is immutable; an OPEN or CALCULATED one can be recalculated and always
 * yields the same numbers for the same inputs because parameters are resolved by period date.
 */
@Service
public class PayrollService {

    private final PayrollPeriodRepository periods;
    private final NoveltyRepository novelties;
    private final PayslipRepository payslips;
    private final ContractRepository contracts;
    private final EmployeeService employeeService;
    private final LegalParameterService legalParameters;
    private final PayrollMapper mapper;
    private final Clock clock;

    public PayrollService(PayrollPeriodRepository periods, NoveltyRepository novelties, PayslipRepository payslips,
                          ContractRepository contracts, EmployeeService employeeService,
                          LegalParameterService legalParameters, PayrollMapper mapper, Clock clock) {
        this.periods = periods;
        this.novelties = novelties;
        this.payslips = payslips;
        this.contracts = contracts;
        this.employeeService = employeeService;
        this.legalParameters = legalParameters;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ periods

    @Transactional(readOnly = true)
    public List<PeriodResponse> listPeriods() {
        return periods.findAllByOrderByYearDescMonthDesc().stream()
                .map(p -> mapper.toResponse(p, payslips.countByPeriodId(p.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public PeriodResponse getPeriod(Long id) {
        PayrollPeriod p = findPeriod(id);
        return mapper.toResponse(p, payslips.countByPeriodId(id));
    }

    @Transactional
    public PeriodResponse createPeriod(PeriodRequest request) {
        if (periods.findByYearAndMonth(request.year(), request.month()).isPresent()) {
            throw new ConflictException("El periodo " + request.year() + "-" + request.month() + " ya existe");
        }
        PayrollPeriod p = new PayrollPeriod();
        p.setYear(request.year());
        p.setMonth(request.month());
        p.setStatus(PeriodStatus.OPEN);
        return mapper.toResponse(periods.save(p), 0);
    }

    // ------------------------------------------------------------------ novelties

    @Transactional(readOnly = true)
    public List<NoveltyResponse> listNovelties(Long periodId) {
        findPeriod(periodId);
        return novelties.findByPeriodId(periodId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public NoveltyResponse addNovelty(Long periodId, NoveltyRequest request) {
        PayrollPeriod period = findEditablePeriod(periodId);
        Employee employee = employeeService.find(request.employeeId());
        validateNovelty(request);
        Novelty n = new Novelty();
        n.setPeriod(period);
        n.setEmployee(employee);
        n.setNoveltyType(request.noveltyType());
        n.setQuantity(request.quantity() == null ? BigDecimal.ZERO : request.quantity());
        n.setAmount(request.amount() == null ? BigDecimal.ZERO : request.amount());
        n.setDescription(request.description());
        markStale(period);
        return mapper.toResponse(novelties.save(n));
    }

    @Transactional
    public void deleteNovelty(Long periodId, Long noveltyId) {
        PayrollPeriod period = findEditablePeriod(periodId);
        Novelty n = novelties.findById(noveltyId)
                .filter(x -> x.getPeriod().getId().equals(periodId))
                .orElseThrow(() -> new NotFoundException("Novedad", noveltyId));
        novelties.delete(n);
        markStale(period);
    }

    // ------------------------------------------------------------------ calculation

    @Transactional
    public PayrollSheetResponse calculate(Long periodId) {
        PayrollPeriod period = findEditablePeriod(periodId);
        YearMonth ym = YearMonth.of(period.getYear(), period.getMonth());
        LegalParameterSet params = legalParameters.loadSet(ym.atEndOfMonth());

        payslips.deleteAll(payslips.findByPeriodId(periodId));
        payslips.flush();

        Map<Long, List<Novelty>> byEmployee = novelties.findByPeriodId(periodId).stream()
                .collect(Collectors.groupingBy(n -> n.getEmployee().getId()));
        List<Contract> active = contracts.findActiveBetween(ym.atDay(1), ym.atEndOfMonth());
        for (Contract contract : active) {
            Employee employee = contract.getEmployee();
            PayrollInput input = toInput(ym, contract, employee, byEmployee.getOrDefault(employee.getId(), List.of()));
            PayrollResult result;
            try {
                result = PayrollCalculator.calculate(input, params);
            } catch (RuntimeException ex) {
                throw new BusinessRuleException(EmployeeMapper.fullName(employee) + ": " + ex.getMessage());
            }
            payslips.save(toEntity(period, contract, employee, result, params));
        }
        period.setStatus(PeriodStatus.CALCULATED);
        period.setCalculatedAt(OffsetDateTime.now(clock));
        payslips.flush();
        return sheet(periodId);
    }

    @Transactional
    public PeriodResponse close(Long periodId, String username) {
        PayrollPeriod period = findPeriod(periodId);
        if (period.getStatus() != PeriodStatus.CALCULATED) {
            throw new ConflictException("Solo se puede cerrar un periodo calculado (estado actual: " + period.getStatus() + ")");
        }
        YearMonth ym = YearMonth.of(period.getYear(), period.getMonth());
        boolean previousOpen = periods.findAll().stream()
                .anyMatch(p -> YearMonth.of(p.getYear(), p.getMonth()).isBefore(ym) && p.getStatus() != PeriodStatus.CLOSED);
        if (previousOpen) {
            throw new ConflictException("Cierre primero los periodos anteriores");
        }
        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(OffsetDateTime.now(clock));
        period.setClosedBy(username);
        return mapper.toResponse(period, payslips.countByPeriodId(periodId));
    }

    @Transactional(readOnly = true)
    public PayrollSheetResponse sheet(Long periodId) {
        PayrollPeriod period = findPeriod(periodId);
        List<Payslip> list = payslips.findByPeriodId(periodId);
        return mapper.toSheet(mapper.toResponse(period, list.size()), list);
    }

    @Transactional(readOnly = true)
    public PayslipResponse payslip(Long payslipId) {
        return mapper.toResponse(findPayslip(payslipId));
    }

    @Transactional(readOnly = true)
    public Payslip findPayslip(Long payslipId) {
        Payslip p = payslips.findDetailed(payslipId).orElseThrow(() -> new NotFoundException("Rol de pago", payslipId));
        p.getLines().size();
        p.getProvisions().size();
        return p;
    }

    @Transactional(readOnly = true)
    public List<Payslip> findPayslipsOfPeriod(Long periodId) {
        findPeriod(periodId);
        List<Payslip> list = payslips.findByPeriodId(periodId);
        list.forEach(p -> p.getLines().size());
        return list;
    }

    // ------------------------------------------------------------------ helpers

    private PayrollInput toInput(YearMonth ym, Contract c, Employee e, List<Novelty> items) {
        int year = ym.getYear();
        int month = ym.getMonthValue();
        return new PayrollInput(ym, c.getMonthlySalary(), c.getWeeklyHours(), c.getStartDate(), c.getEndDate(),
                c.getThirteenthMode(), c.getFourteenthMode(), c.getReserveFundMode(),
                sum(items, NoveltyType.OVERTIME_SUPPLEMENTARY, true),
                sum(items, NoveltyType.OVERTIME_EXTRAORDINARY, true),
                sum(items, NoveltyType.ABSENCE_DAYS, true),
                sum(items, NoveltyType.BONUS, false),
                sum(items, NoveltyType.ADVANCE, false),
                sum(items, NoveltyType.OTHER_DEDUCTION, false),
                employeeService.personalExpenses(e.getId(), year),
                e.getFamilyDependents(), e.isCatastrophicCondition(),
                payslips.sumTaxableBefore(e.getId(), year, month),
                payslips.sumWithheldBefore(e.getId(), year, month));
    }

    private static BigDecimal sum(List<Novelty> items, NoveltyType type, boolean quantity) {
        return items.stream().filter(n -> n.getNoveltyType() == type)
                .map(n -> quantity ? n.getQuantity() : n.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payslip toEntity(PayrollPeriod period, Contract contract, Employee employee, PayrollResult r, LegalParameterSet params) {
        Payslip p = new Payslip();
        p.setPeriod(period);
        p.setContract(contract);
        p.setEmployee(employee);
        p.setWorkedDays(r.workedDays());
        p.setBaseSalary(r.baseSalary());
        p.setIessBase(r.iessBase());
        p.setIncomeTaxBase(r.incomeTaxBase());
        p.setProjectedAnnualTaxBase(r.projectedAnnualTaxBase());
        p.setTotalIncome(r.totalIncome());
        p.setTotalDeductions(r.totalDeductions());
        p.setNetPay(r.netPay());
        p.setEmployerCost(r.employerCost());
        p.setParametersSnapshot(params.toSnapshot());
        p.setCalculatedAt(OffsetDateTime.now(clock));
        int order = 0;
        for (PayrollResult.Line line : r.lines()) {
            PayslipLine l = new PayslipLine();
            l.setPayslip(p);
            l.setConceptCode(line.conceptCode());
            l.setQuantity(line.quantity());
            l.setRate(line.rate());
            l.setAmount(line.amount());
            l.setSortOrder(order++);
            p.getLines().add(l);
        }
        for (PayrollResult.Provision prov : r.provisions()) {
            Provision entity = new Provision();
            entity.setPayslip(p);
            entity.setEmployeeId(employee.getId());
            entity.setPeriodId(period.getId());
            entity.setProvisionType(prov.type());
            entity.setAmount(prov.amount());
            entity.setPaidMonthly(prov.paidMonthly());
            p.getProvisions().add(entity);
        }
        return p;
    }

    private static void validateNovelty(NoveltyRequest r) {
        boolean hourOrDay = switch (r.noveltyType()) {
            case OVERTIME_SUPPLEMENTARY, OVERTIME_EXTRAORDINARY, ABSENCE_DAYS -> true;
            case ADVANCE, BONUS, OTHER_DEDUCTION -> false;
        };
        BigDecimal relevant = hourOrDay ? r.quantity() : r.amount();
        if (relevant == null || relevant.signum() <= 0) {
            throw new BusinessRuleException(hourOrDay
                    ? "La novedad " + r.noveltyType() + " requiere una cantidad (horas o días) mayor a cero"
                    : "La novedad " + r.noveltyType() + " requiere un monto mayor a cero");
        }
    }

    private void markStale(PayrollPeriod period) {
        if (period.getStatus() == PeriodStatus.CALCULATED) {
            period.setStatus(PeriodStatus.OPEN);
        }
    }

    private PayrollPeriod findPeriod(Long id) {
        return periods.findById(id).orElseThrow(() -> new NotFoundException("Periodo", id));
    }

    private PayrollPeriod findEditablePeriod(Long id) {
        PayrollPeriod p = findPeriod(id);
        if (p.getStatus() == PeriodStatus.CLOSED) {
            throw new ConflictException("El periodo " + p.getYear() + "-" + p.getMonth() + " está cerrado y no admite cambios");
        }
        return p;
    }

}
