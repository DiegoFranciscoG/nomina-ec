package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.dto.LegalParameterDtos.AuditResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.LegalParameterResponse;
import com.dgranda.nominaec.dto.LegalParameterDtos.NewVersionRequest;
import com.dgranda.nominaec.dto.LegalParameterDtos.TaxTablesResponse;
import com.dgranda.nominaec.service.LegalParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/legal-parameters")
@Tag(name = "Parámetros legales")
public class LegalParameterController {

    private final LegalParameterService service;
    private final Clock clock;

    public LegalParameterController(LegalParameterService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Todas las versiones de todos los parámetros, con su vigencia y fuente")
    public List<LegalParameterResponse> list() {
        return service.list(LocalDate.now(clock));
    }

    @GetMapping("/{code}/history")
    public List<LegalParameterResponse> history(@PathVariable @Pattern(regexp = "^[A-Z0-9_]{2,60}$") String code) {
        return service.history(code, LocalDate.now(clock));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea una nueva versión (cierra la vigente el día anterior). No requiere recompilar.")
    public LegalParameterResponse createVersion(@Valid @RequestBody NewVersionRequest request, Authentication auth) {
        return service.createVersion(request, auth.getName(), LocalDate.now(clock));
    }

    @GetMapping("/audit")
    @Operation(summary = "Bitácora de cambios de parámetros: quién, cuándo, antes/después y motivo")
    public Page<AuditResponse> audit(@RequestParam(required = false) String code,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.audit(code, PageRequest.of(page, size));
    }

    @GetMapping("/tax-tables/{year}")
    @Operation(summary = "Tabla de IR y límites de gastos personales del año fiscal")
    public TaxTablesResponse taxTables(@PathVariable @Min(2000) @Max(2100) int year) {
        return service.taxTables(year);
    }
}
