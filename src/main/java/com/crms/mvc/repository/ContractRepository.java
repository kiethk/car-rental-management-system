package com.crms.mvc.repository;

import com.crms.mvc.entity.Contract;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @EntityGraph(attributePaths = {"template", "booking.customer"})
    Optional<Contract> findByBookingId(Long bookingId);
}
