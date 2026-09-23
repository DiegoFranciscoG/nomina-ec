package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.support.ApiClient;
import com.dgranda.nominaec.support.DatabaseCleaner;
import com.dgranda.nominaec.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementAndSimulationIT extends IntegrationTest {

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

    private long createEmployee(String thirteenth) throws Exception {
        long positionId = jdbc.queryForObject("SELECT id FROM positions WHERE code = 'CONT'", Long.class);
        Map<String, Object> contract = Map.of("positionId", positionId, "contractType", "INDEFINITE", "weeklyHours", 40,
                "monthlySalary", 1000, "startDate", "2023-03-01", "region", "SIERRA_AMAZONIA", "thirteenthMode", thirteenth,
                "fourteenthMode", "ACCUMULATED", "reserveFundMode", "MONTHLY");
        return api.read(api.post(admin, "/api/employees", Map.of("idNumber", "1709991044", "firstNames", "Lucía",
                "lastNames", "Ficticia", "familyDependents", 1, "catastrophicCondition", false, "contract", contract))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void settlementUsesAccumulatedProvisionsAndTerminatesTheContract() throws Exception {
        long employee = createEmployee("ACCUMULATED");
        // Two calculated months provision 1000/12 = 83.33 of thirteenth each
        for (int month = 1; month <= 2; month++) {
            long period = api.read(api.post(admin, "/api/periods", Map.of("year", 2026, "month", month))).get("id").asLong();
            api.post(admin, "/api/periods/" + period + "/calculate", null).andExpect(status().isOk());
            api.post(admin, "/api/periods/" + period + "/close", null).andExpect(status().isOk());
        }
        Map<String, Object> request = Map.of("employeeId", employee, "terminationDate", "2026-03-15",
                "reason", "UNJUSTIFIED_DISMISSAL", "pendingSalaryDays", 15, "unusedVacationDays", 0);

        JsonNode preview = api.read(api.post(admin, "/api/settlements/preview", request).andExpect(status().isOk()));
        JsonNode thirteenth = findItem(preview, "THIRTEENTH");
        // 83.33 + 83.33 accumulated + 500 / 12 of the pending salary = 208.33
        assertThat(thirteenth.get("amount").decimalValue()).isEqualByComparingTo("208.33");
        // 3 years and 15 days of service -> fraction counts as a full year -> 4 salaries
        assertThat(findItem(preview, "DISMISSAL_INDEMNITY").get("amount").decimalValue()).isEqualByComparingTo("4000.00");

        api.post(admin, "/api/settlements", request).andExpect(status().isCreated());
        api.get(admin, "/api/employees/" + employee).andExpect(jsonPath("$.contract.status").value("TERMINATED"))
                .andExpect(jsonPath("$.active").value(false));
        api.post(admin, "/api/settlements", request).andExpect(status().isUnprocessableEntity());
        api.get(admin, "/api/settlements").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void settlementPdfPreview() throws Exception {
        long employee = createEmployee("MONTHLY");
        byte[] pdf = api.post(admin, "/api/settlements/preview.pdf", Map.of("employeeId", employee,
                        "terminationDate", "2026-09-30", "reason", "RESIGNATION", "pendingSalaryDays", 30))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void hiringCostSimulator() throws Exception {
        String demo = api.token(ApiClient.DEMO, ApiClient.DEMO_PASSWORD);
        api.post(demo, "/api/simulations/hiring-cost", Map.of("monthlySalary", 482, "weeklyHours", 40,
                        "referenceDate", "2026-01-15", "thirteenthMode", "MONTHLY", "fourteenthMode", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstYear.monthlyCost").value(640.98))
                .andExpect(jsonPath("$.fromSecondYear.monthlyCost").value(681.13))
                .andExpect(jsonPath("$.belowMinimumWage").value(false));
        api.post(demo, "/api/simulations/hiring-cost", Map.of("monthlySalary", -5, "weeklyHours", 40,
                "thirteenthMode", "MONTHLY", "fourteenthMode", "MONTHLY")).andExpect(status().isBadRequest());
    }

    private static JsonNode findItem(JsonNode settlement, String code) {
        for (JsonNode item : settlement.get("items")) {
            if (item.get("code").asText().equals(code)) {
                return item;
            }
        }
        throw new AssertionError("item " + code + " not found");
    }
}
