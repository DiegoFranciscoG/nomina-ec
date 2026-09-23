package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query("""
            select p from Payslip p join fetch p.employee e join fetch p.contract c join fetch c.position
            where p.period.id = :periodId order by e.lastNames, e.firstNames
            """)
    List<Payslip> findByPeriodId(@Param("periodId") Long periodId);

    @Query("""
            select p from Payslip p join fetch p.employee join fetch p.period
            join fetch p.contract c join fetch c.position where p.id = :id
            """)
    Optional<Payslip> findDetailed(@Param("id") Long id);

    @Query("""
            select coalesce(sum(p.incomeTaxBase), 0) from Payslip p
            where p.employee.id = :employeeId and p.period.year = :year and p.period.month < :month
            """)
    BigDecimal sumTaxableBefore(@Param("employeeId") Long employeeId, @Param("year") int year, @Param("month") int month);

    @Query("""
            select coalesce(sum(l.amount), 0) from PayslipLine l
            where l.payslip.employee.id = :employeeId and l.payslip.period.year = :year
              and l.payslip.period.month < :month and l.conceptCode = 'INCOME_TAX'
            """)
    BigDecimal sumWithheldBefore(@Param("employeeId") Long employeeId, @Param("year") int year, @Param("month") int month);

    long countByPeriodId(Long periodId);
}
