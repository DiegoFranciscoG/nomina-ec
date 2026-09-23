package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Provision;
import com.dgranda.nominaec.entity.ProvisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProvisionRepository extends JpaRepository<Provision, Long> {

    /** Accumulated (not paid monthly) provision of an employee between two yyyymm keys, inclusive. */
    @Query("""
            select coalesce(sum(p.amount), 0) from Provision p join p.payslip s join s.period per
            where p.employeeId = :employeeId and p.provisionType = :type and p.paidMonthly = false
              and (per.year * 100 + per.month) between :fromKey and :toKey
            """)
    BigDecimal sumAccumulated(@Param("employeeId") Long employeeId, @Param("type") ProvisionType type,
                              @Param("fromKey") int fromKey, @Param("toKey") int toKey);

    @Query("""
            select p.provisionType as type, p.paidMonthly as paidMonthly, sum(p.amount) as total
            from Provision p join p.payslip s join s.period per
            where p.employeeId = :employeeId and per.year = :year
            group by p.provisionType, p.paidMonthly
            """)
    List<ProvisionTotal> totalsByYear(@Param("employeeId") Long employeeId, @Param("year") int year);

    interface ProvisionTotal {
        ProvisionType getType();

        Boolean getPaidMonthly();

        BigDecimal getTotal();
    }
}
