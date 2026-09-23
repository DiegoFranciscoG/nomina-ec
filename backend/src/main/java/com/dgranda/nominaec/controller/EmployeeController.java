package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.dto.EmployeeDtos.BenefitBalanceResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.ContractUpdateRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeUpdateRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.PersonalExpenseRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.PositionResponse;
import com.dgranda.nominaec.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Personal")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping("/employees")
    public List<EmployeeResponse> list() {
        return service.list();
    }

    @GetMapping("/employees/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    @Operation(summary = "Registra un empleado con su contrato (valida cédula y SBU vigente)")
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return service.create(request);
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/employees/{id}/contract")
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    @Operation(summary = "Actualiza sueldo, jornada y modalidad de décimos / fondos de reserva")
    public EmployeeResponse updateContract(@PathVariable Long id, @Valid @RequestBody ContractUpdateRequest request) {
        return service.updateContract(id, request);
    }

    @PutMapping("/employees/{id}/personal-expenses")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    @Operation(summary = "Registra la proyección de gastos personales del año (formulario SRI)")
    public void savePersonalExpenses(@PathVariable Long id, @Valid @RequestBody PersonalExpenseRequest request) {
        service.savePersonalExpenses(id, request);
    }

    @GetMapping("/employees/{id}/personal-expenses")
    public BigDecimal personalExpenses(@PathVariable Long id, @RequestParam @Min(2000) @Max(2100) int year) {
        return service.personalExpenses(id, year);
    }

    @GetMapping("/employees/{id}/benefits")
    @Operation(summary = "Décimos y fondos de reserva: acumulado vs. mensualizado en el año")
    public BenefitBalanceResponse benefits(@PathVariable Long id, @RequestParam @Min(2000) @Max(2100) int year) {
        return service.benefitBalance(id, year);
    }

    @GetMapping("/positions")
    public List<PositionResponse> positions() {
        return service.positions();
    }
}
