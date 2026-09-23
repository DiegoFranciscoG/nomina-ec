package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.support.ApiClient;
import com.dgranda.nominaec.support.DatabaseCleaner;
import com.dgranda.nominaec.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end payroll through the REST API and PostgreSQL. Expected amounts are the hand-verified
 * values of PayrollCalculatorTest (case A and case B).
 */
class PayrollFlowIT extends IntegrationTest {

    @Autowired
    ApiClient api;
    @Autowired
    DatabaseCleaner cleaner;
    @Autowired
    JdbcTemplate jdbc;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        cleaner.reset();
        admin = api.token(ApiClient.ADMIN, ApiClient.ADMIN_PASSWORD);
    }

    private long positionId() {
        return jdbc.queryForObject("SELECT id FROM positions WHERE code = 'DES-SW'", Long.class);
    }

    private long createEmployee(String cedula, String salary, String start, String thirteenth, String fourteenth,
                                String reserve) throws Exception {
        Map<String, Object> contract = new HashMap<>();
        contract.put("positionId", positionId());
        contract.put("contractType", "INDEFINITE");
        contract.put("weeklyHours", 40);
        contract.put("monthlySalary", new BigDecimal(salary));
        contract.put("startDate", start);
        contract.put("region", "SIERRA_AMAZONIA");
        contract.put("thirteenthMode", thirteenth);
        contract.put("fourteenthMode", fourteenth);
        contract.put("reserveFundMode", reserve);
        Map<String, Object> body = Map.of("idNumber", cedula, "firstNames", "Prueba", "lastNames", "Ficticia",
                "email", "prueba@empresa-demo.ec", "familyDependents", 0, "catastrophicCondition", false, "contract", contract);
        JsonNode created = api.read(api.post(admin, "/api/employees", body).andExpect(status().isCreated()));
        assertThat(created.get("idNumberMasked").asText()).startsWith("17").contains("*");
        return created.get("id").asLong();
    }

    private long createPeriod(int month) throws Exception {
        return api.read(api.post(admin, "/api/periods", Map.of("year", 2026, "month", month))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private JsonNode payslipOf(long periodId, long employeeId) throws Exception {
        JsonNode sheet = api.read(api.get(admin, "/api/periods/" + periodId + "/payroll").andExpect(status().isOk()));
        for (JsonNode row : sheet.get("payslips")) {
            if (row.get("employeeId").asLong() == employeeId) {
                return api.read(api.get(admin, "/api/payslips/" + row.get("id").asLong()).andExpect(status().isOk()));
            }
        }
        throw new AssertionError("payslip not found");
    }

    private static BigDecimal line(JsonNode payslip, String code) {
        for (JsonNode l : payslip.get("lines")) {
            if (l.get("conceptCode").asText().equals(code)) {
                return l.get("amount").decimalValue();
            }
        }
        return BigDecimal.ZERO;
    }

    @Test
    void caseA_overtimeAccumulatedBenefitsAndReserveFund() throws Exception {
        long employee = createEmployee("1709991010", "1200.00", "2023-03-01", "ACCUMULATED", "ACCUMULATED", "MONTHLY");
        api.put(admin, "/api/employees/" + employee + "/personal-expenses", Map.of("fiscalYear", 2026, "projectedAmount", 3000))
                .andExpect(status().isNoContent());
        long period = createPeriod(1);
        api.post(admin, "/api/periods/" + period + "/novelties",
                Map.of("employeeId", employee, "noveltyType", "OVERTIME_SUPPLEMENTARY", "quantity", 10)).andExpect(status().isCreated());
        api.post(admin, "/api/periods/" + period + "/novelties",
                Map.of("employeeId", employee, "noveltyType", "OVERTIME_EXTRAORDINARY", "quantity", 4)).andExpect(status().isCreated());

        api.post(admin, "/api/periods/" + period + "/calculate", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.status").value("CALCULATED"));

        JsonNode p = payslipOf(period, employee);
        assertThat(line(p, "SALARY")).isEqualByComparingTo("1200.00");
        assertThat(line(p, "OVERTIME_SUPPLEMENTARY")).isEqualByComparingTo("75.00");
        assertThat(line(p, "OVERTIME_EXTRAORDINARY")).isEqualByComparingTo("40.00");
        assertThat(line(p, "IESS_PERSONAL")).isEqualByComparingTo("124.27");
        assertThat(line(p, "PROV_THIRTEENTH")).isEqualByComparingTo("109.58");
        assertThat(line(p, "PROV_FOURTEENTH")).isEqualByComparingTo("40.17");
        assertThat(line(p, "RESERVE_FUND_MONTHLY")).isEqualByComparingTo("109.54");
        assertThat(line(p, "INCOME_TAX")).isZero();
        assertThat(p.get("netPay").decimalValue()).isEqualByComparingTo("1300.27");
        assertThat(p.get("employerCost").decimalValue()).isEqualByComparingTo("1788.86");
        assertThat(p.get("parametersSnapshot").get("parameters").get("SBU").decimalValue()).isEqualByComparingTo("482");

        // Benefit balance: accumulated thirteenth and fourteenth, reserve fund paid monthly
        api.get(admin, "/api/employees/" + employee + "/benefits?year=2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thirteenthAccumulated").value(109.58))
                .andExpect(jsonPath("$.reserveFundPaid").value(109.54));
    }

    @Test
    void caseB_incomeTaxIsReliquidatedAcrossMonths() throws Exception {
        long employee = createEmployee("1709991028", "2500.00", "2025-06-01", "MONTHLY", "MONTHLY", "MONTHLY");
        api.put(admin, "/api/employees/" + employee + "/personal-expenses", Map.of("fiscalYear", 2026, "projectedAmount", 2000))
                .andExpect(status().isNoContent());

        long january = createPeriod(1);
        api.post(admin, "/api/periods/" + january + "/calculate", null).andExpect(status().isOk());
        assertThat(line(payslipOf(january, employee), "INCOME_TAX")).isEqualByComparingTo("93.48");
        api.post(admin, "/api/periods/" + january + "/close", null).andExpect(status().isOk());

        long february = createPeriod(2);
        api.post(admin, "/api/periods/" + february + "/calculate", null).andExpect(status().isOk());
        JsonNode feb = payslipOf(february, employee);
        // (1121.75 - 93.48) / 11 = 93.48, using January's taxable income and withholding from the database
        assertThat(line(feb, "INCOME_TAX")).isEqualByComparingTo("93.48");
        assertThat(feb.get("projectedAnnualTaxBase").decimalValue()).isEqualByComparingTo("27165.00");
    }

    @Test
    void recalculationIsReproducibleAndClosedPeriodsAreImmutable() throws Exception {
        long employee = createEmployee("1709991036", "1234.56", "2020-02-29", "ACCUMULATED", "MONTHLY", "ACCUMULATED");
        long period = createPeriod(5);
        api.post(admin, "/api/periods/" + period + "/novelties",
                Map.of("employeeId", employee, "noveltyType", "OVERTIME_SUPPLEMENTARY", "quantity", 7.5)).andExpect(status().isCreated());

        JsonNode first = api.read(api.post(admin, "/api/periods/" + period + "/calculate", null).andExpect(status().isOk()));
        JsonNode second = api.read(api.post(admin, "/api/periods/" + period + "/calculate", null).andExpect(status().isOk()));
        assertThat(second.get("totals")).isEqualTo(first.get("totals"));
        assertThat(second.get("payslips").get(0).get("netPay")).isEqualTo(first.get("payslips").get(0).get("netPay"));

        api.post(admin, "/api/periods/" + period + "/close", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedBy").value("admin"));
        api.post(admin, "/api/periods/" + period + "/calculate", null).andExpect(status().isConflict());
        api.post(admin, "/api/periods/" + period + "/novelties",
                Map.of("employeeId", employee, "noveltyType", "BONUS", "amount", 10)).andExpect(status().isConflict());

        // A new parameter version after the closed period does not alter the stored payslip
        JsonNode before = payslipOf(period, employee);
        api.post(admin, "/api/legal-parameters", Map.of("code", "IESS_PERSONAL_RATE", "value", 0.10,
                "validFrom", "2026-06-01", "legalBasis", "Reforma de prueba", "sourceUrl", "https://www.iess.gob.ec/",
                "reason", "Prueba de reproducibilidad")).andExpect(status().isCreated());
        assertThat(payslipOf(period, employee)).isEqualTo(before);
    }

    @Test
    void newParameterVersionAppliesToLaterPeriodsWithoutRecompiling() throws Exception {
        long employee = createEmployee("1709991010", "1000.00", "2025-09-01", "MONTHLY", "MONTHLY", "MONTHLY");
        api.post(admin, "/api/legal-parameters", Map.of("code", "SBU", "value", 500, "validFrom", "2026-10-01",
                "legalBasis", "Acuerdo de prueba", "sourceUrl", "https://www.trabajo.gob.ec/",
                "reason", "Nuevo SBU simulado")).andExpect(status().isCreated());

        long september = createPeriod(9);
        long october = createPeriod(10);
        api.post(admin, "/api/periods/" + september + "/calculate", null).andExpect(status().isOk());
        api.post(admin, "/api/periods/" + october + "/calculate", null).andExpect(status().isOk());
        assertThat(line(payslipOf(september, employee), "FOURTEENTH_MONTHLY")).isEqualByComparingTo("40.17"); // 482 / 12
        assertThat(line(payslipOf(october, employee), "FOURTEENTH_MONTHLY")).isEqualByComparingTo("41.67");   // 500 / 12

        api.get(admin, "/api/legal-parameters/audit?code=SBU").andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void pdfReportsAreGenerated() throws Exception {
        long employee = createEmployee("1709991010", "900.00", "2024-01-01", "MONTHLY", "MONTHLY", "MONTHLY");
        long period = createPeriod(2);
        api.post(admin, "/api/periods/" + period + "/calculate", null).andExpect(status().isOk());
        long payslipId = payslipOf(period, employee).get("id").asLong();

        byte[] payslipPdf = api.get(admin, "/api/payslips/" + payslipId + "/pdf").andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        byte[] sheetPdf = api.get(admin, "/api/periods/" + period + "/payroll.pdf").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(payslipPdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(new String(sheetPdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void validationErrorsAreReportedWithoutInternals() throws Exception {
        long period = createPeriod(3);
        api.post(admin, "/api/employees", Map.of("idNumber", "1234567890", "firstNames", "X1", "lastNames", "Y"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idNumber").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
        long employee = createEmployee("1709991010", "900.00", "2024-01-01", "MONTHLY", "MONTHLY", "MONTHLY");
        api.post(admin, "/api/periods/" + period + "/novelties",
                Map.of("employeeId", employee, "noveltyType", "OVERTIME_SUPPLEMENTARY", "quantity", 60)).andExpect(status().isCreated());
        api.post(admin, "/api/periods/" + period + "/calculate", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("48")));
        api.post(admin, "/api/periods", Map.of("year", 2026, "month", 3)).andExpect(status().isConflict());
    }

    @Test
    void salaryBelowMinimumWageIsRejected() throws Exception {
        Map<String, Object> contract = Map.of("positionId", positionId(), "contractType", "INDEFINITE", "weeklyHours", 40,
                "monthlySalary", 400, "startDate", "2026-09-01", "region", "COSTA_GALAPAGOS", "thirteenthMode", "MONTHLY",
                "fourteenthMode", "MONTHLY", "reserveFundMode", "MONTHLY");
        api.post(admin, "/api/employees", Map.of("idNumber", "1709991010", "firstNames", "Ana", "lastNames", "Prueba",
                        "familyDependents", 0, "catastrophicCondition", false, "contract", contract))
                .andExpect(status().isUnprocessableEntity());
    }
}
