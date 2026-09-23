package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.IncomeTaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeTaxBracketRepository extends JpaRepository<IncomeTaxBracket, Long> {

    List<IncomeTaxBracket> findByFiscalYearOrderByLowerBound(int fiscalYear);
}
