package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s join fetch s.employee order by s.createdAt desc")
    List<Settlement> findAllDetailed();

    boolean existsByContractId(Long contractId);
}
