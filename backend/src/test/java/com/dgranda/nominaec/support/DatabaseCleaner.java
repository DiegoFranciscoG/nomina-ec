package com.dgranda.nominaec.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Restores the database to the migration seed so that integration tests do not depend on each other. */
@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbc;

    public DatabaseCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void reset() {
        jdbc.execute("""
                TRUNCATE settlements, provisions, payslip_lines, payslips, novelties, payroll_periods,
                         personal_expense_projections, contracts, employees, legal_parameter_audit RESTART IDENTITY CASCADE
                """);
        jdbc.update("DELETE FROM legal_parameters WHERE created_by <> 'system'");
        jdbc.update("UPDATE legal_parameters SET valid_to = NULL WHERE created_by = 'system'");
    }
}
