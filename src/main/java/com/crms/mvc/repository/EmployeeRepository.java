package com.crms.mvc.repository;

import com.crms.mvc.entity.Employee;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Employee findByEmailIgnoreCase(String email);

    @Query("SELECT e FROM Employee e WHERE e.role = 'staff' " +
           "ORDER BY (SELECT count(b) FROM Booking b WHERE b.employee = e AND b.status IN ('confirmed', 'active', 'pending_payment')) ASC, e.id ASC")
    List<Employee> findLeastLoadedStaff(Pageable pageable);
}
