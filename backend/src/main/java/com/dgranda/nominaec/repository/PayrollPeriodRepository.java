package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.PayrollPeriod;
import com.dgranda.nominaec.entity.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByYearAndMonth(int year, int month);

    List<PayrollPeriod> findAllByOrderByYearDescMonthDesc();

    /** True when a period with the given status exists at or after (year, month). */
    @Query("""
            select count(p) > 0 from PayrollPeriod p
            where p.status = :status and (p.year > :year or (p.year = :year and p.month >= :month))
            """)
    boolean existsWithStatusFrom(@Param("status") PeriodStatus status, @Param("year") int year, @Param("month") int month);
}
