package com.dgranda.nominaec.repository;

import com.dgranda.nominaec.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByIdNumber(String idNumber);

    List<Employee> findAllByOrderByLastNamesAscFirstNamesAsc();
}
