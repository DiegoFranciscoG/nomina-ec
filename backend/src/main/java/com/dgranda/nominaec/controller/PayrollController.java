package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.dto.PayrollDtos.NoveltyRequest;
import com.dgranda.nominaec.dto.PayrollDtos.NoveltyResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayrollSheetResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodRequest;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodResponse;
import com.dgranda.nominaec.pdf.PdfReportService;
import com.dgranda.nominaec.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Nómina")
public class PayrollController {

    private final PayrollService service;
    private final PdfReportService pdf;

    public PayrollController(PayrollService service, PdfReportService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping("/periods")
    public List<PeriodResponse> periods() {
        return service.listPeriods();
    }

    @GetMapping("/periods/{id}")
    public PeriodResponse period(@PathVariable Long id) {
        return service.getPeriod(id);
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    public PeriodResponse createPeriod(@Valid @RequestBody PeriodRequest request) {
        return service.createPeriod(request);
    }

    @GetMapping("/periods/{id}/novelties")
    public List<NoveltyResponse> novelties(@PathVariable Long id) {
        return service.listNovelties(id);
    }

    @PostMapping("/periods/{id}/novelties")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    @Operation(summary = "Registra horas extra, faltas, anticipos, bonos u otros descuentos")
    public NoveltyResponse addNovelty(@PathVariable Long id, @Valid @RequestBody NoveltyRequest request) {
        return service.addNovelty(id, request);
    }

    @DeleteMapping("/periods/{id}/novelties/{noveltyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    public void deleteNovelty(@PathVariable Long id, @PathVariable Long noveltyId) {
        service.deleteNovelty(id, noveltyId);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','PAYROLL')")
    @Operation(summary = "Calcula (o recalcula) los roles del periodo con los parámetros vigentes a fin de mes")
    public PayrollSheetResponse calculate(@PathVariable Long id) {
        return service.calculate(id);
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cierra el periodo: los roles quedan inmutables")
    public PeriodResponse close(@PathVariable Long id, Authentication auth) {
        return service.close(id, auth.getName());
    }

    @GetMapping("/periods/{id}/payroll")
    @Operation(summary = "Planilla del periodo con totales")
    public PayrollSheetResponse sheet(@PathVariable Long id) {
        return service.sheet(id);
    }

    @GetMapping(value = "/periods/{id}/payroll.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> sheetPdf(@PathVariable Long id) {
        PayrollSheetResponse sheet = service.sheet(id);
        String name = "planilla-%d-%02d.pdf".formatted(sheet.period().year(), sheet.period().month());
        return pdfResponse(pdf.payrollSheet(sheet), name);
    }

    @GetMapping("/payslips/{id}")
    public PayslipResponse payslip(@PathVariable Long id) {
        return service.payslip(id);
    }

    @GetMapping(value = "/payslips/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Rol de pagos individual en PDF")
    public ResponseEntity<byte[]> payslipPdf(@PathVariable Long id) {
        PayslipResponse p = service.payslip(id);
        return pdfResponse(pdf.payslip(p), "rol-%d-%02d-%d.pdf".formatted(p.year(), p.month(), p.employeeId()));
    }

    static ResponseEntity<byte[]> pdfResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
