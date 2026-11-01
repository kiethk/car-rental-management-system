package com.crms.mvc.repository;

import com.crms.mvc.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE :keyword ESCAPE '\\' OR LOWER(c.email) LIKE :keyword ESCAPE '\\'")
    Page<Customer> search(@Param("keyword") String keyword, Pageable pageable);
}
