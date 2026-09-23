package com.dgranda.nominaec.service;

import com.dgranda.nominaec.config.AppProperties;
import com.dgranda.nominaec.dto.EmployeeDtos.ContractRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeRequest;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.PersonalExpenseRequest;
import com.dgranda.nominaec.dto.PayrollDtos.NoveltyRequest;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodRequest;
import com.dgranda.nominaec.dto.PayrollDtos.PeriodResponse;
import com.dgranda.nominaec.entity.ContractType;
import com.dgranda.nominaec.entity.NoveltyType;
import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.Region;
import com.dgranda.nominaec.repository.EmployeeRepository;
import com.dgranda.nominaec.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads FICTITIOUS demo data (names and id numbers are invented; id numbers only satisfy the
 * checksum) and runs the real payroll flow for the months of the current year. Enabled with DEMO_DATA=true.
 */
@Service
@Order(2)
public class DemoDataService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final AppProperties properties;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positions;
    private final EmployeeService employees;
    private final PayrollService payroll;
    private final Clock clock;

    public DemoDataService(AppProperties properties, EmployeeRepository employeeRepository, PositionRepository positions,
                           EmployeeService employees, PayrollService payroll, Clock clock) {
        this.properties = properties;
        this.employeeRepository = employeeRepository;
        this.positions = positions;
        this.employees = employees;
        this.payroll = payroll;
        this.clock = clock;
    }

    private record Person(String first, String last, String position, String salary, LocalDate start, Region region,
                          PaymentMode thirteenth, PaymentMode fourteenth, PaymentMode reserve, int dependents,
                          int weeklyHours, String personalExpenses) {
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.demoData() || employeeRepository.count() > 0) {
            return;
        }
        log.info("Loading fictitious demo data");
        List<Person> people = List.of(
                new Person("Valeria Sofía", "Andrade Paredes", "GER-GEN", "3200.00", LocalDate.of(2019, 2, 1), Region.SIERRA_AMAZONIA,
                        PaymentMode.ACCUMULATED, PaymentMode.ACCUMULATED, PaymentMode.ACCUMULATED, 2, 40, "9000.00"),
                new Person("Mateo Alejandro", "Cevallos Rivas", "JEF-TH", "1850.00", LocalDate.of(2021, 6, 14), Region.SIERRA_AMAZONIA,
                        PaymentMode.MONTHLY, PaymentMode.ACCUMULATED, PaymentMode.MONTHLY, 1, 40, "5200.00"),
                new Person("Camila Fernanda", "Loor Zambrano", "ANA-NOM", "1100.00", LocalDate.of(2023, 3, 1), Region.COSTA_GALAPAGOS,
                        PaymentMode.MONTHLY, PaymentMode.MONTHLY, PaymentMode.MONTHLY, 0, 40, "3000.00"),
                new Person("Sebastián Andrés", "Vera Montesdeoca", "DES-SW", "2400.00", LocalDate.of(2025, 4, 16), Region.SIERRA_AMAZONIA,
                        PaymentMode.MONTHLY, PaymentMode.MONTHLY, PaymentMode.MONTHLY, 0, 40, "2000.00"),
                new Person("Daniela Isabel", "Guamán Tenesaca", "CONT", "1350.00", LocalDate.of(2022, 9, 1), Region.SIERRA_AMAZONIA,
                        PaymentMode.ACCUMULATED, PaymentMode.ACCUMULATED, PaymentMode.MONTHLY, 3, 40, "4000.00"),
                new Person("Luis Fernando", "Macías Bravo", "VEN", "650.00", LocalDate.of(2024, 11, 4), Region.COSTA_GALAPAGOS,
                        PaymentMode.MONTHLY, PaymentMode.MONTHLY, PaymentMode.MONTHLY, 2, 40, "0"),
                new Person("José Miguel", "Quishpe Chango", "OPE-BOD", "482.00", LocalDate.of(2026, 2, 16), Region.SIERRA_AMAZONIA,
                        PaymentMode.MONTHLY, PaymentMode.MONTHLY, PaymentMode.MONTHLY, 1, 40, "0"),
                new Person("Ana Lucía", "Torres Vélez", "ASI-ADM", "300.00", LocalDate.of(2025, 8, 1), Region.COSTA_GALAPAGOS,
                        PaymentMode.MONTHLY, PaymentMode.MONTHLY, PaymentMode.MONTHLY, 0, 20, "0"));

        List<EmployeeResponse> created = new ArrayList<>();
        int seq = 1;
        for (Person p : people) {
            Long positionId = positions.findByCode(p.position()).orElseThrow().getId();
            ContractRequest contract = new ContractRequest(positionId, ContractType.INDEFINITE, p.weeklyHours(),
                    new BigDecimal(p.salary()), p.start(), p.region(), p.thirteenth(), p.fourteenth(), p.reserve());
            String email = p.first().split(" ")[0].toLowerCase() + "." + p.last().split(" ")[0].toLowerCase() + "@empresa-demo.ec";
            EmployeeResponse e = employees.create(new EmployeeRequest(fictitiousCedula(seq++), p.first(), p.last(),
                    stripAccents(email), p.dependents(), false, contract));
            created.add(e);
            if (new BigDecimal(p.personalExpenses()).signum() > 0) {
                employees.savePersonalExpenses(e.id(), new PersonalExpenseRequest(LocalDate.now(clock).getYear(),
                        new BigDecimal(p.personalExpenses())));
            }
        }

        YearMonth current = YearMonth.now(clock);
        for (int month = 1; month <= current.getMonthValue(); month++) {
            PeriodResponse period = payroll.createPeriod(new PeriodRequest(current.getYear(), month));
            addNovelties(period.id(), month, created);
            payroll.calculate(period.id());
            if (month < current.getMonthValue()) {
                payroll.close(period.id(), properties.bootstrap().adminUsername());
            }
        }
        log.info("Demo data loaded: {} employees, {} periods", created.size(), current.getMonthValue());
    }

    private void addNovelties(Long periodId, int month, List<EmployeeResponse> e) {
        novelty(periodId, e.get(3).id(), NoveltyType.OVERTIME_SUPPLEMENTARY, 6 + month % 5, null, "Soporte a despliegue");
        novelty(periodId, e.get(6).id(), NoveltyType.OVERTIME_EXTRAORDINARY, month % 3 == 0 ? 8 : 4, null, "Inventario de fin de semana");
        novelty(periodId, e.get(5).id(), NoveltyType.BONUS, null, 120 + 10 * month, "Comisión por ventas");
        if (month % 4 == 0) {
            novelty(periodId, e.get(2).id(), NoveltyType.ABSENCE_DAYS, 1, null, "Falta injustificada");
        }
        if (month % 2 == 1) {
            novelty(periodId, e.get(1).id(), NoveltyType.ADVANCE, null, 200, "Anticipo quincenal");
        }
    }

    private void novelty(Long periodId, Long employeeId, NoveltyType type, Integer quantity, Integer amount, String description) {
        payroll.addNovelty(periodId, new NoveltyRequest(employeeId, type,
                quantity == null ? null : BigDecimal.valueOf(quantity),
                amount == null ? null : BigDecimal.valueOf(amount), description));
    }

    /** Province 17 + fixed prefix + sequence + modulo-10 check digit. Not tied to any real person. */
    static String fictitiousCedula(int sequence) {
        String nine = "17" + "0" + String.format("%06d", 999000 + sequence);
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int d = (nine.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            sum += d > 9 ? d - 9 : d;
        }
        return nine + ((10 - sum % 10) % 10);
    }

    private static String stripAccents(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
