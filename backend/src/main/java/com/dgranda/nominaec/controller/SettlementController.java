package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.dto.SettlementDtos.SettlementRequest;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementResponse;
import com.dgranda.nominaec.pdf.PdfReportService;
import com.dgranda.nominaec.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
@Tag(name = "Liquidaciones")
public class SettlementController {

    private final SettlementService service;
    private final PdfReportService pdf;

    public SettlementController(SettlementService service, PdfReportService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @PostMapping("/preview")
    @Operation(summary = "Calcula la liquidación sin guardar nada")
    public SettlementResponse preview(@Valid @RequestBody SettlementRequest request) {
        return service.preview(request);
    }

    @PostMapping(value = "/preview.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewPdf(@Valid @RequestBody SettlementRequest request) {
        SettlementResponse s = service.preview(request);
        return PayrollController.pdfResponse(pdf.settlement(s), "liquidacion-" + s.employeeId() + ".pdf");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra la liquidación y termina el contrato")
    public SettlementResponse register(@Valid @RequestBody SettlementRequest request, Authentication auth) {
        return service.register(request, auth.getName());
    }
}
