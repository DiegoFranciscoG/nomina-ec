package com.dgranda.nominaec.mapper;

import com.dgranda.nominaec.dto.EmployeeDtos.ContractResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.EmployeeResponse;
import com.dgranda.nominaec.dto.EmployeeDtos.PositionResponse;
import com.dgranda.nominaec.entity.Contract;
import com.dgranda.nominaec.entity.Employee;
import com.dgranda.nominaec.entity.Position;
import com.dgranda.nominaec.service.validation.CedulaValidator;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee e, Contract contract) {
        return new EmployeeResponse(e.getId(), CedulaValidator.mask(e.getIdNumber()), e.getFirstNames(), e.getLastNames(),
                e.getEmail(), e.getFamilyDependents(), e.isCatastrophicCondition(), e.isActive(),
                contract == null ? null : toResponse(contract));
    }

    public ContractResponse toResponse(Contract c) {
        return new ContractResponse(c.getId(), c.getPosition().getId(), c.getPosition().getName(), c.getContractType(),
                c.getWeeklyHours(), c.getMonthlySalary(), c.getStartDate(), c.getEndDate(), c.getRegion(),
                c.getThirteenthMode(), c.getFourteenthMode(), c.getReserveFundMode(), c.getStatus());
    }

    public PositionResponse toResponse(Position p) {
        return new PositionResponse(p.getId(), p.getCode(), p.getName());
    }

    public static String fullName(Employee e) {
        return e.getLastNames() + " " + e.getFirstNames();
    }
}
