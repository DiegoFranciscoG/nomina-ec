package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Contract;
import com.dgranda.nominaec.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findFirstByEmployeeIdAndStatus(Long employeeId, ContractStatus status);

    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    /** Contracts that overlap the period [from, to]: they drive who gets a payslip. */
    @Query("""
            select c from Contract c join fetch c.employee e join fetch c.position
            where c.startDate <= :to and (c.endDate is null or c.endDate >= :from)
            order by e.lastNames, e.firstNames
            """)
    List<Contract> findActiveBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
