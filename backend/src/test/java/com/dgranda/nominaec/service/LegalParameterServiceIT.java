package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.ParameterCode;
import com.dgranda.nominaec.dto.LegalParameterDtos.NewVersionRequest;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodRequest;
import com.dgranda.nominaec.entity.AuditAction;
import com.dgranda.nominaec.exception.BusinessRuleException;
import com.dgranda.nominaec.exception.ConflictException;
import com.dgranda.nominaec.repository.PayrollPeriodRepository;
import com.dgranda.nominaec.support.DatabaseCleaner;
import com.dgranda.nominaec.support.IntegrationTest;
import com.dgranda.nominaec.support.TestParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.dgranda.nominaec.support.TestParameters.bd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegalParameterServiceIT extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);
    private static final String SOURCE = "https://www.trabajo.gob.ec/";

    @Autowired
    LegalParameterService service;
    @Autowired
    PayrollService payrollService;
    @Autowired
    PayrollPeriodRepository periods;
    @Autowired
    DatabaseCleaner cleaner;

    @BeforeEach
    void reset() {
        cleaner.reset();
    }

    @Test
    void migrationSeedMatchesTheResearchedValues() {
        LegalParameterSet fromDb = service.loadSet(LocalDate.of(2026, 1, 31));
        LegalParameterSet expected = TestParameters.y2026();
        for (ParameterCode code : ParameterCode.values()) {
            assertThat(fromDb.get(code)).as(code.name()).isEqualByComparingTo(expected.get(code));
        }
        assertThat(fromDb.taxBrackets()).hasSize(10);
        assertThat(fromDb.taxBrackets()).usingRecursiveFieldByFieldElementComparator(
                        org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration.builder()
                                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class).build())
                .containsExactlyElementsOf(expected.taxBrackets());
        assertThat(fromDb.expenseCapBaskets(0)).isEqualByComparingTo("7");
        assertThat(fromDb.expenseCapBaskets(9)).isEqualByComparingTo("20");
    }

    @Test
    void newVersionClosesThePreviousOneAndIsAudited() {
        var request = new NewVersionRequest("SBU", bd("500.00"), LocalDate.of(2026, 11, 1), null,
                "Acuerdo ministerial de prueba", SOURCE, "Simulación de reforma salarial");
        service.createVersion(request, "admin", TODAY);

        assertThat(service.loadSet(LocalDate.of(2026, 10, 31)).get(ParameterCode.SBU)).isEqualByComparingTo("482");
        assertThat(service.loadSet(LocalDate.of(2026, 11, 30)).get(ParameterCode.SBU)).isEqualByComparingTo("500");

        var history = service.history("SBU", TODAY);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).validTo()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(history.get(1).inForce()).isTrue();
        assertThat(history.get(0).inForce()).isFalse();

        var audit = service.audit("SBU", PageRequest.of(0, 10)).getContent();
        assertThat(audit).extracting(a -> a.action()).containsExactlyInAnyOrder(AuditAction.CREATE, AuditAction.CLOSE_VALIDITY);
        assertThat(audit).allMatch(a -> a.changedBy().equals("admin") && a.reason().startsWith("Simulación"));
        var close = audit.stream().filter(a -> a.action() == AuditAction.CLOSE_VALIDITY).findFirst().orElseThrow();
        assertThat(close.oldValue()).containsEntry("validTo", null);
        assertThat(close.newValue()).containsEntry("validTo", "2026-10-31");
    }

    @Test
    void rejectsVersionStartingBeforeTheCurrentOne() {
        var request = new NewVersionRequest("SBU", bd("470"), LocalDate.of(2025, 12, 1), null, "x", SOURCE,
                "Intento de retroactividad");
        assertThatThrownBy(() -> service.createVersion(request, "admin", TODAY)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsVersionThatWouldAlterAClosedPeriod() {
        var period = payrollService.createPeriod(new PeriodRequest(2026, 3));
        payrollService.calculate(period.id());
        payrollService.close(period.id(), "admin");

        var request = new NewVersionRequest("IESS_PERSONAL_RATE", bd("0.10"), LocalDate.of(2026, 3, 15), null, "x", SOURCE,
                "Cambio que tocaría marzo");
        assertThatThrownBy(() -> service.createVersion(request, "admin", TODAY)).isInstanceOf(ConflictException.class);
    }

    @Test
    void brandNewParameterNeedsDescription() {
        var noDescription = new NewVersionRequest("NEW_BENEFIT_RATE", bd("0.01"), LocalDate.of(2026, 10, 1), null, "x", SOURCE,
                "Parámetro nuevo sin descripción");
        assertThatThrownBy(() -> service.createVersion(noDescription, "admin", TODAY)).isInstanceOf(BusinessRuleException.class);

        var ok = new NewVersionRequest("NEW_BENEFIT_RATE", bd("0.01"), LocalDate.of(2026, 10, 1), "Nuevo beneficio", "x", SOURCE,
                "Parámetro nuevo con descripción");
        assertThat(service.createVersion(ok, "admin", TODAY).code()).isEqualTo("NEW_BENEFIT_RATE");
    }

    @Test
    void taxTablesExposeBracketsAndPersonalExpenseCaps() {
        var tables = service.taxTables(2026);
        assertThat(tables.brackets()).hasSize(10);
        assertThat(tables.personalExpenseCaps()).hasSize(6);
        assertThat(tables.personalExpenseCaps().get(0).maxAmount()).isEqualByComparingTo("5752.60");
        assertThat(tables.personalExpenseCaps().get(0).maxRebate()).isEqualByComparingTo("1035.47");
    }
}
