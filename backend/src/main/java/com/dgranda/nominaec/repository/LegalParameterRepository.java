package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.LegalParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LegalParameterRepository extends JpaRepository<LegalParameter, Long> {

    @Query("""
            select p from LegalParameter p
            where p.validFrom <= :date and (p.validTo is null or p.validTo >= :date)
            """)
    List<LegalParameter> findInForceOn(@Param("date") LocalDate date);

    List<LegalParameter> findByCodeOrderByValidFromDesc(String code);

    List<LegalParameter> findAllByOrderByCodeAscValidFromDesc();

    @Query("select p from LegalParameter p where p.code = :code and p.validTo is null")
    Optional<LegalParameter> findOpenEnded(@Param("code") String code);
}
