package com.crms.mvc.repository;

import com.crms.mvc.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Employee findByEmailIgnoreCase(String email);

    @Query("SELECT e FROM Employee e WHERE e.role = 'staff' AND e.active = true " +
           "ORDER BY (SELECT count(b) FROM Booking b WHERE b.employee = e AND b.status IN ('confirmed', 'active', 'pending_payment')) ASC, e.id ASC")
    List<Employee> findLeastLoadedStaff(Pageable pageable);

    List<Employee> findByActiveTrueOrderByNameAsc();

    @Query("SELECT e FROM Employee e WHERE LOWER(e.name) LIKE :keyword ESCAPE '\\' OR LOWER(e.email) LIKE :keyword ESCAPE '\\'")
    Page<Employee> search(@Param("keyword") String keyword, Pageable pageable);
}
