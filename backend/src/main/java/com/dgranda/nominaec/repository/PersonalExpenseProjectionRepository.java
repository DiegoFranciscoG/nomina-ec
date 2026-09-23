package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.PersonalExpenseProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalExpenseProjectionRepository extends JpaRepository<PersonalExpenseProjection, Long> {

    Optional<PersonalExpenseProjection> findByEmployeeIdAndFiscalYear(Long employeeId, int fiscalYear);
}
