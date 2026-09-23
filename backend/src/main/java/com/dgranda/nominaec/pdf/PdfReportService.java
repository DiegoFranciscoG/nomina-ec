package com.dgranda.nominaec.pdf;

import com.dgranda.nominaec.dto.PayrollDtos.PayrollSheetResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipLineResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipResponse;
import com.dgranda.nominaec.dto.PayrollDtos.PayslipSummary;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementItemResponse;
import com.dgranda.nominaec.dto.SettlementDtos.SettlementResponse;
import com.dgranda.nominaec.entity.ConceptType;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Payslip, payroll sheet and settlement PDFs rendered with OpenPDF. */
@Service
public class PdfReportService {

    private static final Locale ES_EC = Locale.forLanguageTag("es-EC");
    private static final Color BRAND = new Color(0x1F, 0x4E, 0x79);
    private static final Color LIGHT = new Color(0xEE, 0xF3, 0xF8);
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BRAND);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    private final String companyName;

    public PdfReportService(@Value("${app.company-name}") String companyName) {
        this.companyName = companyName;
    }

    public byte[] payslip(PayslipResponse p) {
        return render(PageSize.A4, doc -> {
            doc.add(new Paragraph(companyName, TITLE));
            doc.add(new Paragraph("ROL DE PAGOS — " + monthName(p.month()) + " " + p.year(), BOLD));
            doc.add(Chunk.NEWLINE);

            PdfPTable info = table(new float[]{1, 2, 1, 2});
            infoRow(info, "Empleado", p.employeeName(), "Cédula", p.idNumberMasked());
            infoRow(info, "Cargo", p.positionName(), "Días trabajados", number(p.workedDays()));
            infoRow(info, "Sueldo", money(p.baseSalary()), "Base IESS", money(p.iessBase()));
            doc.add(info);
            doc.add(Chunk.NEWLINE);

            PdfPTable lines = table(new float[]{4, 1, 1.5f});
            headers(lines, "Ingresos", "Cant.", "Valor");
            addLines(lines, p.lines(), l -> l.conceptType() == ConceptType.INCOME);
            totalRow(lines, "Total ingresos", p.totalIncome());
            headers(lines, "Descuentos", "", "Valor");
            addLines(lines, p.lines(), l -> l.conceptType() == ConceptType.DEDUCTION);
            totalRow(lines, "Total descuentos", p.totalDeductions());
            totalRow(lines, "NETO A RECIBIR", p.netPay());
            doc.add(lines);
            doc.add(Chunk.NEWLINE);

            PdfPTable employer = table(new float[]{4, 1, 1.5f});
            headers(employer, "Aportes patronales y provisiones (informativo)", "", "Valor");
            addLines(employer, p.lines(), l -> l.conceptType() == ConceptType.EMPLOYER_CONTRIBUTION
                    || l.conceptType() == ConceptType.PROVISION);
            totalRow(employer, "Costo total del empleador", p.employerCost());
            doc.add(employer);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Base gravada IR del mes: " + money(p.incomeTaxBase())
                    + "   ·   Proyección anual: " + money(p.projectedAnnualTaxBase()), SMALL));
            doc.add(new Paragraph("Calculado con los parámetros legales vigentes al " + p.parametersSnapshot().get("effectiveDate")
                    + " (SBU, IESS, SRI). Documento generado por nomina-ec.", SMALL));
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);
            PdfPTable signatures = table(new float[]{1, 1});
            signatures.addCell(signature("Firma del empleador"));
            signatures.addCell(signature("Recibí conforme"));
            doc.add(signatures);
        });
    }

    public byte[] payrollSheet(PayrollSheetResponse sheet) {
        return render(PageSize.A4.rotate(), doc -> {
            doc.add(new Paragraph(companyName, TITLE));
            doc.add(new Paragraph("PLANILLA DE NÓMINA — " + monthName(sheet.period().month()) + " " + sheet.period().year()
                    + "   (estado: " + sheet.period().status() + ")", BOLD));
            doc.add(Chunk.NEWLINE);
            PdfPTable t = table(new float[]{3, 2.2f, 0.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.3f});
            headers(t, "Empleado", "Cargo", "Días", "Sueldo", "Base IESS", "Ingresos", "Descuentos", "Neto", "Costo empleador");
            for (PayslipSummary s : sheet.payslips()) {
                t.addCell(cell(s.employeeName(), NORMAL, Element.ALIGN_LEFT));
                t.addCell(cell(s.positionName(), NORMAL, Element.ALIGN_LEFT));
                t.addCell(cell(number(s.workedDays()), NORMAL, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.baseSalary()), NORMAL, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.iessBase()), NORMAL, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.totalIncome()), NORMAL, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.totalDeductions()), NORMAL, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.netPay()), BOLD, Element.ALIGN_RIGHT));
                t.addCell(cell(money(s.employerCost()), NORMAL, Element.ALIGN_RIGHT));
            }
            var totals = sheet.totals();
            PdfPCell label = cell("TOTALES (" + sheet.payslips().size() + " empleados)", BOLD, Element.ALIGN_LEFT);
            label.setColspan(5);
            label.setBackgroundColor(LIGHT);
            t.addCell(label);
            for (BigDecimal v : List.of(totals.totalIncome(), totals.totalDeductions(), totals.netPay(), totals.employerCost())) {
                PdfPCell c = cell(money(v), BOLD, Element.ALIGN_RIGHT);
                c.setBackgroundColor(LIGHT);
                t.addCell(c);
            }
            doc.add(t);
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Aporte personal IESS: " + money(totals.iessPersonal()) + "   ·   Aporte patronal IESS: "
                    + money(totals.iessEmployer()) + "   ·   Retenciones IR: " + money(totals.incomeTax())
                    + "   ·   Provisiones: " + money(totals.provisions()), NORMAL));
        });
    }

    public byte[] settlement(SettlementResponse s) {
        return render(PageSize.A4, doc -> {
            doc.add(new Paragraph(companyName, TITLE));
            doc.add(new Paragraph("LIQUIDACIÓN DE HABERES", BOLD));
            doc.add(Chunk.NEWLINE);
            PdfPTable info = table(new float[]{1, 2, 1, 2});
            infoRow(info, "Empleado", s.employeeName(), "Motivo", s.reason().name());
            infoRow(info, "Ingreso", s.startDate().toString(), "Salida", s.terminationDate().toString());
            infoRow(info, "Tiempo de servicio", s.serviceDays() + " días (" + s.serviceYears() + " años)",
                    "Última remuneración", money(s.lastSalary()));
            doc.add(info);
            doc.add(Chunk.NEWLINE);
            PdfPTable t = table(new float[]{5, 1.5f});
            headers(t, "Concepto", "Valor");
            for (SettlementItemResponse item : s.items()) {
                t.addCell(cell(item.description(), NORMAL, Element.ALIGN_LEFT));
                t.addCell(cell(money(item.amount()), NORMAL, Element.ALIGN_RIGHT));
            }
            PdfPCell label = cell("TOTAL A PAGAR", BOLD, Element.ALIGN_LEFT);
            label.setBackgroundColor(LIGHT);
            t.addCell(label);
            PdfPCell total = cell(money(s.netTotal()), BOLD, Element.ALIGN_RIGHT);
            total.setBackgroundColor(LIGHT);
            t.addCell(total);
            doc.add(t);
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Cálculo según Código del Trabajo Arts. 69, 71, 111, 113, 185 y 188. "
                    + "Documento referencial generado por nomina-ec.", SMALL));
        });
    }

    // ------------------------------------------------------------------ helpers

    private interface Body {
        void write(Document doc) throws Exception;
    }

    private static byte[] render(Rectangle size, Body body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(size, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("nomina-ec");
            doc.open();
            body.write(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        return t;
    }

    private static void headers(PdfPTable t, String... titles) {
        for (String title : titles) {
            PdfPCell c = cell(title, HEADER, Element.ALIGN_LEFT);
            c.setBackgroundColor(BRAND);
            t.addCell(c);
        }
    }

    private static void infoRow(PdfPTable t, String k1, String v1, String k2, String v2) {
        t.addCell(cell(k1, BOLD, Element.ALIGN_LEFT));
        t.addCell(cell(v1, NORMAL, Element.ALIGN_LEFT));
        t.addCell(cell(k2, BOLD, Element.ALIGN_LEFT));
        t.addCell(cell(v2, NORMAL, Element.ALIGN_LEFT));
    }

    private static void addLines(PdfPTable t, List<PayslipLineResponse> lines, Predicate<PayslipLineResponse> filter) {
        lines.stream().filter(filter).forEach(l -> {
            t.addCell(cell(l.conceptName(), NORMAL, Element.ALIGN_LEFT));
            t.addCell(cell(l.quantity() == null ? "" : number(l.quantity()), NORMAL, Element.ALIGN_RIGHT));
            t.addCell(cell(money(l.amount()), NORMAL, Element.ALIGN_RIGHT));
        });
    }

    private static void totalRow(PdfPTable t, String label, BigDecimal value) {
        PdfPCell c = cell(label, BOLD, Element.ALIGN_LEFT);
        c.setColspan(2);
        c.setBackgroundColor(LIGHT);
        t.addCell(c);
        PdfPCell v = cell(money(value), BOLD, Element.ALIGN_RIGHT);
        v.setBackgroundColor(LIGHT);
        t.addCell(v);
    }

    private static PdfPCell cell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(Color.LIGHT_GRAY);
        return c;
    }

    private static PdfPCell signature(String text) {
        PdfPCell c = new PdfPCell(new Phrase("\n\n____________________________\n" + text, NORMAL));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    static String money(BigDecimal value) {
        DecimalFormat f = new DecimalFormat("$ #,##0.00", DecimalFormatSymbols.getInstance(ES_EC));
        return f.format(value == null ? BigDecimal.ZERO : value);
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String monthName(int month) {
        String name = Month.of(month).getDisplayName(TextStyle.FULL, ES_EC);
        return name.substring(0, 1).toUpperCase(ES_EC) + name.substring(1);
    }
}
