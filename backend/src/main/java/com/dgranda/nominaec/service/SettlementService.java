package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.SettlementCalculator;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementItemResponse;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementRequest;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementResponse;
import com.dgranda.nominaec.entity.Contract;
import com.dgranda.nominaec.entity.ContractStatus;
import com.dgranda.nominaec.entity.Employee;
import com.dgranda.nominaec.entity.ProvisionType;
import com.dgranda.nominaec.entity.Settlement;
import com.dgranda.nominaec.exception.BusinessRuleException;
import com.dgranda.nominaec.exception.ConflictException;
import com.dgranda.nominaec.mapper.EmployeeMapper;
import com.dgranda.nominaec.repository.ContractRepository;
import com.dgranda.nominaec.repository.ProvisionRepository;
import com.dgranda.nominaec.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Final settlement: preview (no side effects) and registration (terminates the contract). */
@Service
public class SettlementService {

    private final ContractRepository contracts;
    private final ProvisionRepository provisions;
    private final SettlementRepository settlements;
    private final EmployeeService employeeService;
    private final LegalParameterService legalParameters;

    public SettlementService(ContractRepository contracts, ProvisionRepository provisions, SettlementRepository settlements,
                             EmployeeService employeeService, LegalParameterService legalParameters) {
        this.contracts = contracts;
        this.provisions = provisions;
        this.settlements = settlements;
        this.employeeService = employeeService;
        this.legalParameters = legalParameters;
    }

    @Transactional(readOnly = true)
    public SettlementResponse preview(SettlementRequest request) {
        Employee employee = employeeService.find(request.employeeId());
        Contract contract = activeContract(employee.getId());
        SettlementCalculator.Result result = compute(request, contract);
        return toResponse(null, employee, contract, request, result, null);
    }

    @Transactional
    public SettlementResponse register(SettlementRequest request, String username) {
        Employee employee = employeeService.find(request.employeeId());
        Contract contract = activeContract(employee.getId());
        if (settlements.existsByContractId(contract.getId())) {
            throw new ConflictException("El contrato ya tiene una liquidación registrada");
        }
        SettlementCalculator.Result result = compute(request, contract);

        contract.setEndDate(request.terminationDate());
        contract.setStatus(ContractStatus.TERMINATED);
        employee.setActive(false);

        Settlement s = new Settlement();
        s.setEmployee(employee);
        s.setContract(contract);
        s.setTerminationDate(request.terminationDate());
        s.setReason(request.reason());
        s.setTotal(result.netTotal());
        s.setDetail(detail(result, contract));
        s.setCreatedBy(username);
        s = settlements.saveAndFlush(s);
        return toResponse(s.getId(), employee, contract, request, result, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return settlements.findAllDetailed().stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("employeeId", s.getEmployee().getId());
            row.put("employeeName", EmployeeMapper.fullName(s.getEmployee()));
            row.put("terminationDate", s.getTerminationDate());
            row.put("reason", s.getReason());
            row.put("total", s.getTotal());
            row.put("createdBy", s.getCreatedBy());
            row.put("createdAt", s.getCreatedAt());
            return row;
        }).toList();
    }

    private SettlementCalculator.Result compute(SettlementRequest request, Contract contract) {
        if (request.terminationDate().isBefore(contract.getStartDate())) {
            throw new BusinessRuleException("La fecha de salida es anterior al inicio del contrato");
        }
        LegalParameterSet params = legalParameters.loadSet(request.terminationDate());
        LocalDate thirteenthCycleStart = thirteenthCycleStart(request.terminationDate());
        int fromKey = thirteenthCycleStart.getYear() * 100 + thirteenthCycleStart.getMonthValue();
        int toKey = request.terminationDate().getYear() * 100 + request.terminationDate().getMonthValue();
        BigDecimal accumulatedThirteenth = provisions.sumAccumulated(contract.getEmployee().getId(), ProvisionType.THIRTEENTH, fromKey, toKey);

        SettlementCalculator.Input input = new SettlementCalculator.Input(contract.getMonthlySalary(), contract.getWeeklyHours(),
                contract.getStartDate(), request.terminationDate(), request.reason(), contract.getRegion(),
                contract.getThirteenthMode(), contract.getFourteenthMode(), accumulatedThirteenth,
                request.pendingSalaryDays(), request.unusedVacationDays());
        return SettlementCalculator.calculate(input, params);
    }

    /** Thirteenth cycle runs from December 1 of the previous year to November 30 (Art. 111). */
    static LocalDate thirteenthCycleStart(LocalDate date) {
        return date.getMonthValue() == 12 ? LocalDate.of(date.getYear(), 12, 1) : LocalDate.of(date.getYear() - 1, 12, 1);
    }

    private Contract activeContract(Long employeeId) {
        return contracts.findFirstByEmployeeIdAndStatus(employeeId, ContractStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("El empleado no tiene un contrato activo"));
    }

    private static Map<String, Object> detail(SettlementCalculator.Result r, Contract contract) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("lastSalary", contract.getMonthlySalary());
        d.put("startDate", contract.getStartDate().toString());
        d.put("serviceDays", r.serviceDays());
        d.put("serviceYears", r.serviceYears());
        d.put("items", r.items());
        d.put("totalIncome", r.totalIncome());
        d.put("totalDeductions", r.totalDeductions());
        return d;
    }

    private static SettlementResponse toResponse(Long id, Employee e, Contract c, SettlementRequest request,
                                                 SettlementCalculator.Result r, OffsetDateTime createdAt) {
        return new SettlementResponse(id, e.getId(), EmployeeMapper.fullName(e), c.getStartDate(), request.terminationDate(),
                request.reason(), r.serviceDays(), r.serviceYears(), c.getMonthlySalary(),
                r.items().stream().map(i -> new SettlementItemResponse(i.code(), i.description(), i.amount())).toList(),
                r.totalIncome(), r.totalDeductions(), r.netTotal(), createdAt);
    }
}
