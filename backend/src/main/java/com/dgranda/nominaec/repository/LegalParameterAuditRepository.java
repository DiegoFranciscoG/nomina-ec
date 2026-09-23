package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.LegalParameterAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalParameterAuditRepository extends JpaRepository<LegalParameterAudit, Long> {

    Page<LegalParameterAudit> findAllByOrderByChangedAtDescIdDesc(Pageable pageable);

    Page<LegalParameterAudit> findByCodeOrderByChangedAtDescIdDesc(String code, Pageable pageable);
}
