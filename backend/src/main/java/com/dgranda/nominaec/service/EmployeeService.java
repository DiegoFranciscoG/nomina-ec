package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.Money;
import com.dgranda.nominaec.calculation.ParameterCode;
import com.dgranda.nominaec.dto.EmployeeDtos.BenefitBalanceResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.ContractRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.ContractUpdateRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeUpdateRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.PersonalExpenseRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.PositionResponse;
import com.dgranda.nominaec.entity.Contract;
import com.dgranda.nominaec.entity.ContractStatus;
import com.dgranda.nominaec.entity.Employee;
import com.dgranda.nominaec.entity.PersonalExpenseProjection;
import com.dgranda.nominaec.entity.Position;
import com.dgranda.nominaec.entity.ProvisionType;
import com.dgranda.nominaec.exception.BusinessRuleException;
import com.dgranda.nominaec.exception.ConflictException;
import com.dgranda.nominaec.exception.NotFoundException;
import com.dgranda.nominaec.mapper.EmployeeMapper;
import com.dgranda.nominaec.repository.ContractRepository;
import com.dgranda.nominaec.repository.EmployeeRepository;
import com.dgranda.nominaec.repository.PersonalExpenseProjectionRepository;
import com.dgranda.nominaec.repository.PositionRepository;
import com.dgranda.nominaec.repository.ProvisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final ContractRepository contracts;
    private final PositionRepository positions;
    private final PersonalExpenseProjectionRepository projections;
    private final ProvisionRepository provisions;
    private final LegalParameterService legalParameters;
    private final EmployeeMapper mapper;
    private final Clock clock;

    public EmployeeService(EmployeeRepository employees, ContractRepository contracts, PositionRepository positions,
                           PersonalExpenseProjectionRepository projections, ProvisionRepository provisions,
                           LegalParameterService legalParameters, EmployeeMapper mapper, Clock clock) {
        this.employees = employees;
        this.contracts = contracts;
        this.positions = positions;
        this.projections = projections;
        this.provisions = provisions;
        this.legalParameters = legalParameters;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return employees.findAllByOrderByLastNamesAscFirstNamesAsc().stream()
                .map(e -> mapper.toResponse(e, currentContract(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        Employee e = find(id);
        return mapper.toResponse(e, currentContract(id));
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> positions() {
        return positions.findByActiveTrueOrderByName().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employees.existsByIdNumber(request.idNumber())) {
            throw new ConflictException("Ya existe un empleado con esa cédula");
        }
        Employee e = new Employee();
        e.setIdNumber(request.idNumber());
        e.setFirstNames(request.firstNames().trim());
        e.setLastNames(request.lastNames().trim());
        e.setEmail(request.email());
        e.setFamilyDependents(request.familyDependents());
        e.setCatastrophicCondition(request.catastrophicCondition());
        e.setActive(true);
        e = employees.save(e);
        Contract c = newContract(e, request.contract());
        return mapper.toResponse(e, c);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee e = find(id);
        e.setFirstNames(request.firstNames().trim());
        e.setLastNames(request.lastNames().trim());
        e.setEmail(request.email());
        e.setFamilyDependents(request.familyDependents());
        e.setCatastrophicCondition(request.catastrophicCondition());
        return mapper.toResponse(e, currentContract(id));
    }

    @Transactional
    public EmployeeResponse updateContract(Long employeeId, ContractUpdateRequest request) {
        Employee e = find(employeeId);
        Contract c = contracts.findFirstByEmployeeIdAndStatus(employeeId, ContractStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("El empleado no tiene contrato activo"));
        validateMinimumWage(request.monthlySalary(), request.weeklyHours());
        c.setMonthlySalary(request.monthlySalary());
        c.setWeeklyHours(request.weeklyHours());
        c.setThirteenthMode(request.thirteenthMode());
        c.setFourteenthMode(request.fourteenthMode());
        c.setReserveFundMode(request.reserveFundMode());
        return mapper.toResponse(e, c);
    }

    @Transactional
    public void savePersonalExpenses(Long employeeId, PersonalExpenseRequest request) {
        Employee e = find(employeeId);
        PersonalExpenseProjection p = projections.findByEmployeeIdAndFiscalYear(employeeId, request.fiscalYear())
                .orElseGet(PersonalExpenseProjection::new);
        p.setEmployee(e);
        p.setFiscalYear(request.fiscalYear());
        p.setProjectedAmount(request.projectedAmount());
        projections.save(p);
    }

    @Transactional(readOnly = true)
    public BigDecimal personalExpenses(Long employeeId, int year) {
        return projections.findByEmployeeIdAndFiscalYear(employeeId, year)
                .map(PersonalExpenseProjection::getProjectedAmount).orElse(BigDecimal.ZERO);
    }

    /** Thirteenth / fourteenth / reserve fund split between accumulated (provisioned) and paid monthly. */
    @Transactional(readOnly = true)
    public BenefitBalanceResponse benefitBalance(Long employeeId, int year) {
        find(employeeId);
        BigDecimal[] acc = new BigDecimal[ProvisionType.values().length];
        BigDecimal[] paid = new BigDecimal[ProvisionType.values().length];
        Arrays.fill(acc, Money.ZERO);
        Arrays.fill(paid, Money.ZERO);
        provisions.totalsByYear(employeeId, year).forEach(t -> {
            int i = t.getType().ordinal();
            if (Boolean.TRUE.equals(t.getPaidMonthly())) {
                paid[i] = paid[i].add(t.getTotal());
            } else {
                acc[i] = acc[i].add(t.getTotal());
            }
        });
        return new BenefitBalanceResponse(employeeId, year,
                acc[ProvisionType.THIRTEENTH.ordinal()], paid[ProvisionType.THIRTEENTH.ordinal()],
                acc[ProvisionType.FOURTEENTH.ordinal()], paid[ProvisionType.FOURTEENTH.ordinal()],
                acc[ProvisionType.RESERVE_FUND.ordinal()], paid[ProvisionType.RESERVE_FUND.ordinal()],
                acc[ProvisionType.VACATION.ordinal()]);
    }

    Employee find(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Empleado", id));
    }

    private Contract currentContract(Long employeeId) {
        return contracts.findByEmployeeIdOrderByStartDateDesc(employeeId).stream().findFirst().orElse(null);
    }

    private Contract newContract(Employee e, ContractRequest r) {
        Position position = positions.findById(r.positionId()).orElseThrow(() -> new NotFoundException("Cargo", r.positionId()));
        validateMinimumWage(r.monthlySalary(), r.weeklyHours());
        Contract c = new Contract();
        c.setEmployee(e);
        c.setPosition(position);
        c.setContractType(r.contractType());
        c.setWeeklyHours(r.weeklyHours());
        c.setMonthlySalary(r.monthlySalary());
        c.setStartDate(r.startDate());
        c.setRegion(r.region());
        c.setThirteenthMode(r.thirteenthMode());
        c.setFourteenthMode(r.fourteenthMode());
        c.setReserveFundMode(r.reserveFundMode());
        c.setStatus(ContractStatus.ACTIVE);
        return contracts.save(c);
    }

    /** The salary may not be lower than the SBU in force, proportional to the weekly hours (Art. 117). */
    private void validateMinimumWage(BigDecimal salary, int weeklyHours) {
        LegalParameterSet params = legalParameters.loadSet(LocalDate.now(clock));
        BigDecimal minimum = Money.round(params.get(ParameterCode.SBU)
                .multiply(Money.divide(BigDecimal.valueOf(weeklyHours), params.get(ParameterCode.FULL_TIME_WEEKLY_HOURS))));
        if (salary.compareTo(minimum) < 0) {
            throw new BusinessRuleException("El sueldo " + salary + " es menor al SBU proporcional vigente (" + minimum + ")");
        }
    }
}
