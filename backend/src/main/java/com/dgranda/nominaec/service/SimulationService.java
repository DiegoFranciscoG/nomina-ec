package com.dgranda.nominaec.service;

import com.dgranda.nominaec.calculation.HiringCostSimulator;
import com.dgranda.nominaec.dto.SimulationDtos.HiringCostRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class SimulationService {

    private final LegalParameterService legalParameters;
    private final Clock clock;

    public SimulationService(LegalParameterService legalParameters, Clock clock) {
        this.legalParameters = legalParameters;
        this.clock = clock;
    }

    public HiringCostSimulator.Result hiringCost(HiringCostRequest request) {
        LocalDate date = request.referenceDate() != null ? request.referenceDate() : LocalDate.now(clock);
        YearMonth month = YearMonth.from(date);
        var params = legalParameters.loadSet(month.atEndOfMonth());
        return HiringCostSimulator.simulate(new HiringCostSimulator.Input(request.monthlySalary(), request.weeklyHours(), month,
                request.thirteenthMode(), request.fourteenthMode()), params);
    }
}
