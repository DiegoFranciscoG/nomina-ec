package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByActiveTrueOrderByName();

    Optional<Position> findByCode(String code);
}
