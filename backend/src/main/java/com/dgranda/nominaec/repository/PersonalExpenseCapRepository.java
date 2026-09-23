package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.PersonalExpenseCap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalExpenseCapRepository extends JpaRepository<PersonalExpenseCap, Long> {

    List<PersonalExpenseCap> findByFiscalYearOrderByFamilyDependents(int fiscalYear);
}
