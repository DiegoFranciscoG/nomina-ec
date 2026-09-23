package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    List<Concept> findAllByOrderBySortOrder();
}
