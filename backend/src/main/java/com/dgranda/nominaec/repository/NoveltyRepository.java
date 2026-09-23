package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Novelty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoveltyRepository extends JpaRepository<Novelty, Long> {

    @Query("select n from Novelty n join fetch n.employee e where n.period.id = :periodId order by e.lastNames, n.id")
    List<Novelty> findByPeriodId(@Param("periodId") Long periodId);
}
